package br.com.modoaviao.controller;

import br.com.modoaviao.model.Pedido;
import br.com.modoaviao.repository.PedidoRepository;
import br.com.modoaviao.security.MercadoPagoSignatureValidator;
import br.com.modoaviao.service.EmailAutorizadoService;
import br.com.modoaviao.service.MercadoPagoService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe o webhook da API de Orders do Mercado Pago. Protegida pela
 * assinatura HMAC (x-signature), nao por JWT - o Mercado Pago nao tem (nem
 * deveria ter) um token nosso. Nunca confia so na notificacao pra liberar
 * acesso: sempre reconsulta a Order direto no MP antes de agir.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    // TODO confirmar em teste real com uma order paga de verdade - valor
    // baseado no exemplo da documentacao (status "processed" / status_detail
    // "accredited" para uma Order aprovada).
    private static final String STATUS_PROCESSADO = "processed";

    private static final String STATUS_PEDIDO_PAGO = "pago";

    private final MercadoPagoSignatureValidator signatureValidator;
    private final MercadoPagoService mercadoPagoService;
    private final PedidoRepository pedidoRepository;
    private final EmailAutorizadoService emailAutorizadoService;

    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<Void> receberWebhook(
            @RequestBody(required = false) JsonNode payload,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataIdQuery) {

        // Temporario, so pra confirmar em teste o formato real da
        // notificacao e o campo de status da Order. Remover depois.
        log.info("[MP-WEBHOOK] payload recebido: {} | x-signature={} | x-request-id={} | data.id(query)={}",
                payload, xSignature, xRequestId, dataIdQuery);

        String dataId = (dataIdQuery != null && !dataIdQuery.isBlank())
                ? dataIdQuery
                : textoOuNulo(payload == null ? null : payload.path("data"), "id");

        boolean assinaturaValida = signatureValidator.isValid(xSignature, xRequestId, dataId);

        if (!assinaturaValida) {
            log.warn("[MP-WEBHOOK] Assinatura invalida - notificacao rejeitada. dataId={}", dataId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // A partir daqui a notificacao e legitima (assinatura confere). Uma
        // vez validada, o Mercado Pago precisa ouvir "200, recebido" mesmo
        // quando NAO conseguimos processar o conteudo (order id invalido/de
        // teste, order sem external_reference, pedido que nao existe no
        // nosso banco, erro de rede pro MP, etc.) - um 5xx aqui faz ele
        // reenviar a mesma notificacao indefinidamente, o que nao ajuda em
        // nada quando o problema e dado invalido, nao uma falha transitoria
        // nossa. Falha de processamento vira log, nunca vira erro HTTP.
        try {
            processarNotificacao(payload, dataId);
        } catch (Exception e) {
            log.error("[MP-WEBHOOK] Falha ao processar notificacao (assinatura ja validada) - dataId={}", dataId, e);
        }

        return ResponseEntity.ok().build();
    }

    private void processarNotificacao(JsonNode payload, String dataId) {
        String type = payload == null ? null : textoOuNulo(payload, "type");
        if (!"order".equals(type) || dataId == null || dataId.isBlank()) {
            log.info("[MP-WEBHOOK] Notificacao ignorada (type={} dataId={})", type, dataId);
            return;
        }

        Map<?, ?> order = mercadoPagoService.consultarOrder(dataId);
        Object statusObj = order == null ? null : order.get("status");
        String status = statusObj == null ? null : statusObj.toString();
        log.info("[MP-WEBHOOK] order consultada -> id={} status={}", dataId, status);

        if (!STATUS_PROCESSADO.equals(status)) {
            log.info("[MP-WEBHOOK] Order {} ainda nao esta paga (status={}) - nada a liberar por enquanto",
                    dataId, status);
            return;
        }

        Object externalReferenceObj = order.get("external_reference");
        String externalReference = externalReferenceObj == null ? null : externalReferenceObj.toString();

        if (externalReference == null || externalReference.isBlank()) {
            log.warn("[MP-WEBHOOK] Order paga sem external_reference - nao foi possivel associar a um Pedido. orderId={}",
                    dataId);
            return;
        }

        Optional<Pedido> pedidoOpt = pedidoRepository.findByExternalReference(externalReference);
        if (pedidoOpt.isEmpty()) {
            log.warn("[MP-WEBHOOK] Nenhum Pedido encontrado para externalReference={}", externalReference);
            return;
        }

        Pedido pedido = pedidoOpt.get();

        // Idempotente: se ja processamos essa order antes (o MP reenvia
        // notificacao), nao repete o trabalho - so confirma e sai.
        if (STATUS_PEDIDO_PAGO.equals(pedido.getStatus())) {
            log.info("[MP-WEBHOOK] Pedido {} ja estava pago - notificacao duplicada ignorada", pedido.getId());
            return;
        }

        emailAutorizadoService.liberarEmail(pedido.getEmail(), "mercadopago");
        pedido.setStatus(STATUS_PEDIDO_PAGO);
        pedidoRepository.save(pedido);

        log.info("[MP-WEBHOOK] Email liberado -> {} (pedido id={}, order={})",
                pedido.getEmail(), pedido.getId(), dataId);
    }

    private String textoOuNulo(JsonNode node, String campo) {
        if (node == null) {
            return null;
        }
        JsonNode valor = node.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText(null);
    }
}
