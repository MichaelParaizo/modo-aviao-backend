package br.com.modoaviao.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendPasswordResetCode(String toEmail, String toName, String code) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("Brevo API key nao configurada. Email de recuperacao nao enviado para {}.", toEmail);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", "Modo Avião", "email", "contato@reviewprime.app"),
                    "to", List.of(Map.of("email", toEmail, "name", toName)),
                    "subject", "Código de recuperação de senha — Modo Avião",
                    "htmlContent", buildHtml(toName, code));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(BREVO_API_URL, request, Map.class);

            log.info("Email de recuperacao enviado para {}", toEmail);
        } catch (Exception e) {
            log.error("Erro ao enviar email de recuperacao para {}", toEmail, e);
        }
    }

    private String buildHtml(String nome, String code) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0; padding:0; background-color:#111111; font-family: Arial, Helvetica, sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#111111; padding:32px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#1a1a1a; border-radius:12px; padding:32px; border:1px solid #333333;">
                          <tr>
                            <td align="center" style="padding-bottom:16px;">
                              <span style="font-size:22px; font-weight:bold; color:#d4af37; letter-spacing:1px;">MODO AVIÃO</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="color:#f5f5f5; font-size:16px; line-height:1.5;">
                              <p>Olá, %s.</p>
                              <p>Recebemos um pedido para redefinir a senha da sua conta. Use o código abaixo para continuar:</p>
                            </td>
                          </tr>
                          <tr>
                            <td align="center" style="padding:24px 0;">
                              <span style="display:inline-block; background-color:#d4af37; color:#111111; font-size:32px; font-weight:bold; letter-spacing:8px; padding:16px 24px; border-radius:8px;">%s</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="color:#aaaaaa; font-size:13px; line-height:1.5;">
                              <p>Esse código expira em <strong>15 minutos</strong>.</p>
                              <p>Se você não pediu essa recuperação de senha, pode ignorar este email com segurança — sua senha continua a mesma.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(nome, code);
    }
}
