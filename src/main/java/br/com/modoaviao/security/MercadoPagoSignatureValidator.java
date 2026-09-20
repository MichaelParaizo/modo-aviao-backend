package br.com.modoaviao.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Valida a assinatura HMAC-SHA256 que o Mercado Pago manda no header
 * x-signature de cada webhook, seguindo o algoritmo documentado por eles:
 *
 * 1. Extrai ts e v1 do header x-signature (formato "ts=...,v1=...").
 * 2. Monta o manifesto "id:{data.id};request-id:{x-request-id};ts:{ts};",
 *    omitindo qualquer parte cujo valor de origem nao veio na notificacao.
 * 3. Calcula HMAC-SHA256 do manifesto usando o webhook secret como chave,
 *    em hexadecimal.
 * 4. Compara com v1 em tempo constante (MessageDigest.isEqual), pra nao
 *    vazar informacao por timing attack.
 *
 * Isolado numa classe propria (fora do controller) pelo mesmo motivo do
 * JwtService: e a parte criptografica sensivel, separada da orquestracao
 * HTTP - mais facil de revisar e testar isoladamente.
 */
@Slf4j
@Component
public class MercadoPagoSignatureValidator {

    private static final String ALGORITMO_HMAC = "HmacSHA256";

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    public boolean isValid(String xSignature, String xRequestId, String dataId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("[MP-WEBHOOK] mercadopago.webhook-secret nao configurado - notificacao rejeitada por seguranca");
            return false;
        }

        if (xSignature == null || xSignature.isBlank()) {
            log.warn("[MP-WEBHOOK] Header x-signature ausente - notificacao rejeitada");
            return false;
        }

        String ts = extrairParte(xSignature, "ts");
        String v1 = extrairParte(xSignature, "v1");

        if (ts == null || v1 == null) {
            log.warn("[MP-WEBHOOK] x-signature sem ts ou v1 - notificacao rejeitada. header={}", xSignature);
            return false;
        }

        String manifesto = montarManifesto(dataId, xRequestId, ts);
        String hashCalculado = calcularHmacHex(manifesto);

        boolean valido = constantTimeEquals(hashCalculado, v1);
        log.info("[MP-WEBHOOK] validacao de assinatura -> manifesto='{}' valido={}", manifesto, valido);

        return valido;
    }

    private String extrairParte(String xSignature, String chaveAlvo) {
        for (String parte : xSignature.split(",")) {
            String[] chaveValor = parte.split("=", 2);
            if (chaveValor.length == 2 && chaveValor[0].trim().equals(chaveAlvo)) {
                return chaveValor[1].trim();
            }
        }
        return null;
    }

    private String montarManifesto(String dataId, String xRequestId, String ts) {
        StringBuilder manifesto = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifesto.append("id:").append(dataId.toLowerCase()).append(";");
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifesto.append("request-id:").append(xRequestId).append(";");
        }
        if (ts != null && !ts.isBlank()) {
            manifesto.append("ts:").append(ts).append(";");
        }
        return manifesto.toString();
    }

    private String calcularHmacHex(String manifesto) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC));
            byte[] hash = mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8));
            return paraHex(hash);
        } catch (Exception e) {
            log.error("[MP-WEBHOOK] Erro ao calcular HMAC da notificacao", e);
            return "";
        }
    }

    private String paraHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
