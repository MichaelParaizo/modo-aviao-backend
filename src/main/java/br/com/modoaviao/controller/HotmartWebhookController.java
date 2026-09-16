package br.com.modoaviao.controller;

import br.com.modoaviao.service.EmailAutorizadoService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe o webhook (postback) da Hotmart 2.0.0. Quando o evento e uma compra
 * aprovada, libera o email do comprador na lista de emails autorizados -
 * assim ele consegue criar conta no app sem depender do admin liberar
 * manualmente. Rota publica (sem JWT): quem autentica a requisicao e o
 * Hottok, nao um usuario logado - a Hotmart nao tem (nem deveria ter) um
 * token nosso.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HotmartWebhookController {

    private static final String EVENTO_COMPRA_APROVADA = "PURCHASE_APPROVED";
    private static final String HOTTOK_HEADER = "X-HOTMART-HOTTOK";

    private final EmailAutorizadoService emailAutorizadoService;

    @Value("${hotmart.hottok:}")
    private String hotmartHottok;

    @PostMapping("/webhook/hotmart")
    public ResponseEntity<Void> receberWebhook(@RequestBody JsonNode payload,
            @RequestHeader(value = HOTTOK_HEADER, required = false) String hottokDoHeader) {

        // Temporario, so pra confirmar em teste o formato real que a Hotmart
        // manda (event + onde o email do comprador realmente vem). Remover
        // depois de confirmado em producao.
        log.info("[HOTMART-WEBHOOK] payload recebido: {}", payload);

        String hottokRecebido = (hottokDoHeader != null && !hottokDoHeader.isBlank())
                ? hottokDoHeader
                : textoOuNulo(payload, "hottok");

        if (!hottokValido(hottokRecebido)) {
            log.warn("[HOTMART-WEBHOOK] Hottok ausente ou invalido - requisicao rejeitada");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String event = textoOuNulo(payload, "event");
        log.info("[HOTMART-WEBHOOK] event={}", event);

        if (!EVENTO_COMPRA_APROVADA.equals(event)) {
            // Outros eventos (cancelamento, reembolso, etc.) - a Hotmart so
            // precisa do 200 pra saber que recebemos; nao processamos ainda.
            return ResponseEntity.ok().build();
        }

        JsonNode data = payload.path("data");
        String email = extrairEmailComprador(data);
        String nome = extrairNomeComprador(data);
        log.info("[HOTMART-WEBHOOK] compra aprovada -> email={} nome={}", email, nome);

        if (email == null || email.isBlank()) {
            log.warn("[HOTMART-WEBHOOK] PURCHASE_APPROVED sem email de comprador no payload - ignorado");
            return ResponseEntity.ok().build();
        }

        emailAutorizadoService.liberarEmail(email, "hotmart");

        return ResponseEntity.ok().build();
    }

    private boolean hottokValido(String recebido) {
        return hotmartHottok != null && !hotmartHottok.isBlank()
                && recebido != null && recebido.equals(hotmartHottok);
    }

    /**
     * O email do comprador pode vir em data.buyer.email (formato mais comum
     * do 2.0.0) ou data.purchase.buyer.email, dependendo do evento/config da
     * conta Hotmart - tenta os dois caminhos de forma defensiva.
     */
    private String extrairEmailComprador(JsonNode data) {
        String email = textoOuNulo(data.path("buyer"), "email");
        if (email == null || email.isBlank()) {
            email = textoOuNulo(data.path("purchase").path("buyer"), "email");
        }
        return email == null ? null : email.trim().toLowerCase();
    }

    private String extrairNomeComprador(JsonNode data) {
        String nome = textoOuNulo(data.path("buyer"), "name");
        if (nome == null || nome.isBlank()) {
            nome = textoOuNulo(data.path("purchase").path("buyer"), "name");
        }
        return nome;
    }

    private String textoOuNulo(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText(null);
    }
}
