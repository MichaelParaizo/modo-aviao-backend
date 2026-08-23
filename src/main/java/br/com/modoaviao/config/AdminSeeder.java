package br.com.modoaviao.config;

import br.com.modoaviao.model.Role;
import br.com.modoaviao.model.Usuario;
import br.com.modoaviao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * SOMENTE DESENVOLVIMENTO - @Profile("dev").
 *
 * Garante que exista pelo menos um usuario ADMIN para permitir o primeiro
 * login no painel administrativo. Roda uma vez a cada start da aplicacao e
 * nao faz nada se o admin ja existir (idempotente).
 * ============================================================================
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@modoaviao.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome("Admin");
        admin.setEmail(ADMIN_EMAIL);
        admin.setSenhaHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(Role.ADMIN);
        admin.setAcessoLiberado(true);

        usuarioRepository.save(admin);

        log.warn("[DEV-SEED] Admin criado -> email: {} | senha: {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
