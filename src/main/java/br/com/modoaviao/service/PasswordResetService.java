package br.com.modoaviao.service;

import br.com.modoaviao.exception.CodigoRecuperacaoInvalidoException;
import br.com.modoaviao.model.PasswordResetCode;
import br.com.modoaviao.model.Usuario;
import br.com.modoaviao.repository.PasswordResetCodeRepository;
import br.com.modoaviao.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int EXPIRACAO_MINUTOS = 15;

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * De proposito NUNCA lanca excecao nem sinaliza de forma diferente se o
     * email existe ou nao no banco - se lancasse (ou so respondesse diferente
     * pro caso "nao existe"), qualquer pessoa poderia usar este endpoint para
     * descobrir quais emails tem conta no sistema (enumeracao de usuarios).
     * O metodo sempre "termina bem"; so envia o codigo quando ha, de fato,
     * um Usuario com esse email.
     */
    @Transactional
    public void requestReset(String emailBruto) {
        String email = normalizarEmail(emailBruto);

        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            String code = gerarCodigo();

            PasswordResetCode resetCode = new PasswordResetCode();
            resetCode.setEmail(email);
            resetCode.setCode(code);
            resetCode.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS));
            resetCode.setUsed(false);
            passwordResetCodeRepository.save(resetCode);

            emailService.sendPasswordResetCode(email, usuario.getNome(), code);
        });
    }

    @Transactional
    public void resetPassword(String emailBruto, String code, String novaSenha) {
        String email = normalizarEmail(emailBruto);

        PasswordResetCode resetCode = passwordResetCodeRepository.findByEmailAndCodeAndUsedFalse(email, code)
                .orElseThrow(() -> new CodigoRecuperacaoInvalidoException("Código inválido ou expirado"));

        if (resetCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CodigoRecuperacaoInvalidoException("Código inválido ou expirado");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CodigoRecuperacaoInvalidoException("Código inválido ou expirado"));

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        resetCode.setUsed(true);
        passwordResetCodeRepository.save(resetCode);
    }

    // SecureRandom em vez do Random usado no PsiGestao de referencia: um
    // codigo que da acesso pra trocar senha e sensivel o bastante pra
    // merecer uma fonte de aleatoriedade criptograficamente forte, nao so
    // "boa o suficiente para nao repetir". Random.nextInt(999999) tambem
    // teria o detalhe de nunca sortear exatamente 999999 (limite exclusivo);
    // aqui uso nextInt(1_000_000) para cobrir 000000-999999 por igual.
    private String gerarCodigo() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
