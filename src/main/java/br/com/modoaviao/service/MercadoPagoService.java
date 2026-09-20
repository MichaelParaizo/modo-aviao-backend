package br.com.modoaviao.service;

import br.com.modoaviao.dto.CriarPagamentoResponse;
import br.com.modoaviao.exception.PagamentoIndisponivelException;
import br.com.modoaviao.model.Pedido;
import br.com.modoaviao.repository.PedidoRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private static final String ORDERS_API_URL = "https://api.mercadopago.com/v1/orders";
    private static final String DESCRICAO_PRODUTO = "Modo Avião - Ebook";

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.back-url-base}")
    private String backUrlBase;

    @Value("${app.produto.preco}")
    private String preco;

    private final PedidoRepository pedidoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Salva o Pedido (email <-> externalReference) ANTES de chamar o
     * Mercado Pago, de proposito: se a chamada falhar depois de criada a
     * Order mas antes de responder pra gente, ainda temos o registro local
     * pra reconciliar manualmente. Se falhasse so depois, um erro de rede no
     * meio do caminho deixaria a Order criada no MP sem nenhum rastro local.
     */
    @Transactional
    public CriarPagamentoResponse criarOrder(String emailBruto) {
        String email = normalizarEmail(emailBruto);
        String externalReference = UUID.randomUUID().toString();

        Pedido pedido = new Pedido();
        pedido.setExternalReference(externalReference);
        pedido.setEmail(email);
        pedido.setStatus("criado");
        pedidoRepository.save(pedido);

        Map<String, Object> corpo = montarCorpoOrder(email, externalReference);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(corpo, headers);

        Map<?, ?> resposta;
        try {
            ResponseEntity<Map> httpResponse = restTemplate.postForEntity(ORDERS_API_URL, request, Map.class);
            resposta = httpResponse.getBody();
        } catch (RestClientException e) {
            log.error("Erro ao criar Order no Mercado Pago. externalReference={}", externalReference, e);
            throw new PagamentoIndisponivelException("Não foi possível iniciar o pagamento no momento", e);
        }

        Object checkoutUrlObj = resposta == null ? null : resposta.get("checkout_url");
        if (checkoutUrlObj == null) {
            log.error("Resposta do Mercado Pago sem checkout_url. externalReference={} resposta={}",
                    externalReference, resposta);
            throw new PagamentoIndisponivelException("Não foi possível iniciar o pagamento no momento");
        }

        String checkoutUrl = checkoutUrlObj.toString();
        Object orderIdObj = resposta.get("id");

        pedido.setOrderId(orderIdObj == null ? null : orderIdObj.toString());
        pedidoRepository.save(pedido);

        return new CriarPagamentoResponse(checkoutUrl, externalReference);
    }

    /**
     * Consulta o estado real da Order direto no Mercado Pago - o webhook
     * (Onda 2) NUNCA confia so na notificacao recebida pra liberar acesso;
     * a notificacao so avisa "algo mudou", quem manda de verdade e essa
     * consulta.
     */
    public Map<?, ?> consultarOrder(String orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> httpResponse = restTemplate.exchange(
                    ORDERS_API_URL + "/" + orderId, HttpMethod.GET, request, Map.class);
            return httpResponse.getBody();
        } catch (RestClientException e) {
            log.error("Erro ao consultar Order {} no Mercado Pago", orderId, e);
            throw new PagamentoIndisponivelException("Não foi possível consultar o status do pagamento", e);
        }
    }

    /**
     * Formato da API de Orders (Checkout Pro), conferido contra o SDK Go
     * oficial do Mercado Pago (fonte da verdade dos nomes de campo, ja que a
     * API e nova e a doc web as vezes mistura exemplos de fluxos diferentes).
     * Importante: as URLs de retorno NAO ficam num objeto "back_urls" (isso
     * e da API antiga de Preferences) - na API de Orders ficam soltas dentro
     * de config.online: success_url / pending_url / failure_url.
     */
    private Map<String, Object> montarCorpoOrder(String email, String externalReference) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "online");
        body.put("processing_mode", "manual");
        body.put("total_amount", preco);
        body.put("external_reference", externalReference);
        body.put("description", DESCRICAO_PRODUTO);
        body.put("items", List.of(Map.of(
                "title", DESCRICAO_PRODUTO,
                "unit_price", preco,
                "quantity", 1)));
        body.put("payer", Map.of("email", email));
        body.put("config", Map.of("online", Map.of(
                "success_url", backUrlBase + "/pagamento/sucesso",
                "pending_url", backUrlBase + "/pagamento/pendente",
                "failure_url", backUrlBase + "/pagamento/erro")));
        return body;
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
