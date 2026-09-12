package br.com.modoaviao.service;

import br.com.modoaviao.dto.AuthResponse;
import br.com.modoaviao.dto.LoginRequest;
import br.com.modoaviao.dto.SignupRequest;
import br.com.modoaviao.exception.AcessoNaoAutorizadoException;
import br.com.modoaviao.exception.EmailJaCadastradoException;
import br.com.modoaviao.exception.UsuarioNaoEncontradoException;
import br.com.modoaviao.model.Role;
import br.com.modoaviao.model.Usuario;
import br.com.modoaviao.repository.EmailAutorizadoRepository;
import br.com.modoaviao.repository.UsuarioRepository;
import br.com.modoaviao.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final EmailAutorizadoRepository emailAutorizadoRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse signup(SignupRequest request) {
        String email = normalizarEmail(request.getEmail());

        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        // O app e pago e o pagamento acontece ANTES da conta existir: so
        // deixamos criar conta para emails que ja constam como autorizados
        // (liberados manualmente pelo admin por enquanto; futuramente via
        // webhook de pagamento).
        if (!emailAutorizadoRepository.existsByEmailIgnoreCase(email)) {
            throw new AcessoNaoAutorizadoException(
                    "Este e-mail não tem acesso liberado. Verifique se você concluiu a compra ou use o e-mail "
                            + "utilizado no pagamento.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(email);
        usuario.setWhatsapp(request.getWhatsapp());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(Role.USER);
        usuario.setAcessoLiberado(true);

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);

        return new AuthResponse(token, usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getRole().name());
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizarEmail(request.getEmail());

        // Lanca AuthenticationException (tratada pelo GlobalExceptionHandler) se email/senha nao baterem.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getSenha()));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario autenticado nao encontrado: " + email));

        String token = jwtService.generateToken(usuario);

        return new AuthResponse(token, usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getRole().name());
    }
}
