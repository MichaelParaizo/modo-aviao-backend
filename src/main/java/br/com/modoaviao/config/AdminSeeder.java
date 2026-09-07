package br.com.modoaviao.config;

import br.com.modoaviao.model.Role;
import br.com.modoaviao.model.Usuario;
import br.com.modoaviao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * Garante que exista pelo menos um usuario ADMIN para permitir o primeiro
 * login no painel administrativo. Roda uma vez a cada start da aplicacao e
 * nao faz nada se o admin ja existir (idempotente).
 *
 * Le as credenciais de ADMIN_EMAIL / ADMIN_PASSWORD (variaveis de ambiente).
 * Comportamento depende do profile ativo:
 * - DEV: se as variaveis nao estiverem definidas, cai no fallback
 *   admin@modoaviao.com / admin123 (facilita rodar local sem configurar nada).
 * - PROD (ou qualquer profile != dev): NUNCA usa o fallback. Se
 *   ADMIN_EMAIL/ADMIN_PASSWORD nao estiverem definidas, o seeder loga um
 *   aviso e nao cria admin nenhum - criar um admin com senha padrao
 *   conhecida em producao seria uma porta aberta.
 * ============================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final String FALLBACK_DEV_EMAIL = "admin@modoaviao.com";
    private static final String FALLBACK_DEV_PASSWORD = "admin123";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmailEnv;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPasswordEnv;

    @Override
    public void run(String... args) {
        boolean credenciaisDefinidas = !adminEmailEnv.isBlank() && !adminPasswordEnv.isBlank();
        boolean producao = !environment.matchesProfiles("dev");

        if (producao && !credenciaisDefinidas) {
            log.warn("[ADMIN-SEED] Profile de producao ativo sem ADMIN_EMAIL/ADMIN_PASSWORD definidos - "
                    + "seeder de admin pulado por seguranca (nao cria admin com senha padrao em producao). "
                    + "Defina essas variaveis de ambiente e reinicie para criar o admin.");
            return;
        }

        String email = credenciaisDefinidas ? adminEmailEnv : FALLBACK_DEV_EMAIL;
        String senha = credenciaisDefinidas ? adminPasswordEnv : FALLBACK_DEV_PASSWORD;

        if (usuarioRepository.existsByEmail(email)) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome("Admin");
        admin.setEmail(email);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setRole(Role.ADMIN);
        admin.setAcessoLiberado(true);

        usuarioRepository.save(admin);

        // A senha so vai pro log quando e o fallback de dev (ja publico no
        // codigo de qualquer forma); senha vinda de ADMIN_PASSWORD real
        // nunca e exibida em log, mesmo em dev.
        if (credenciaisDefinidas) {
            log.warn("[ADMIN-SEED] Admin criado -> email: {} (senha definida via ADMIN_PASSWORD, nao exibida no log)",
                    email);
        } else {
            log.warn("[ADMIN-SEED] Admin criado -> email: {} | senha: {} (valores padrao de desenvolvimento)",
                    email, senha);
        }
    }
}
