package br.com.modoaviao.config;

import br.com.modoaviao.security.JwtAuthenticationFilter;
import br.com.modoaviao.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Duas cadeias de seguranca convivendo no mesmo backend:
 *
 * - adminSecurityFilterChain (@Order(1)): so intercepta /admin/**. Login por
 *   formulario + sessao (cookie), como uma app web classica. E avaliada
 *   primeiro porque tem prioridade mais alta.
 * - apiSecurityFilterChain (@Order(2)): pega todo o resto (/auth/**,
 *   /chapters, /me, etc). Continua exatamente como antes: JWT stateless.
 *
 * Ambas reutilizam o mesmo DaoAuthenticationProvider / UserDetailsService /
 * PasswordEncoder - e o mesmo banco de usuarios dos dois lados; o que muda e
 * so o mecanismo (sessao vs token).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider)
            throws Exception {
        http
                .securityMatcher("/admin/**")
                // O admin e servido pelo mesmo dominio do backend (same-origin) -
                // nao ha motivo pra essa cadeia sequer avaliar CORS. Desabilitado
                // explicitamente (em vez de so nao chamar .cors()) para deixar
                // claro que e proposital e blindar contra um futuro refactor que
                // acabe aplicando o corsConfigurationSource aqui tambem.
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login")
                        .permitAll())
                .authenticationProvider(authenticationProvider);
        // CSRF fica habilitado (padrao) de proposito: e uma app web com
        // formularios, e o Thymeleaf inclui o token CSRF automaticamente.

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider)
            throws Exception {
        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Preflight do navegador (CORS) nao deve exigir autenticacao.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/login", "/auth/signup", "/auth/logout",
                                "/auth/forgot-password", "/auth/reset-password").permitAll()
                        // A pessoa ainda nao tem conta nesse ponto - ela informa o
                        // email na landing e vai pagar antes de existir Usuario.
                        .requestMatchers("/pagamento/criar").permitAll()
                        // Protegida pela assinatura HMAC (validada no proprio
                        // controller), nao por JWT.
                        .requestMatchers("/webhook/mercadopago").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"error\":\"Not authenticated. Please provide a valid token.\"}");
                        })
                        // Usuario autenticado, mas sem a role exigida.
                        // Escrevemos a resposta na mao (sem response.sendError()) de proposito: sendError()
                        // dispara um segundo dispatch de erro que pula o JwtAuthenticationFilter e faria
                        // esse 403 virar um 401 enganoso - o mesmo problema que corrigimos no filtro JWT.
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter()
                                    .write("{\"error\":\"Access denied. You don't have permission to access this resource.\"}");
                        }))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
