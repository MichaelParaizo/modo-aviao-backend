package br.com.modoaviao.service;

import br.com.modoaviao.dto.AuthResponse;
import br.com.modoaviao.dto.LoginRequest;
import br.com.modoaviao.dto.SignupRequest;
import br.com.modoaviao.exception.EmailJaCadastradoException;
import br.com.modoaviao.exception.UsuarioNaoEncontradoException;
import br.com.modoaviao.model.Role;
import br.com.modoaviao.model.Usuario;
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
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse signup(SignupRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setWhatsapp(request.getWhatsapp());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(Role.USER);
        usuario.setAcessoLiberado(false);

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);

        return new AuthResponse(token, usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        // Lanca AuthenticationException (tratada pelo GlobalExceptionHandler) se email/senha nao baterem.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha()));

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario autenticado nao encontrado: " + request.getEmail()));

        String token = jwtService.generateToken(usuario);

        return new AuthResponse(token, usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getRole().name());
    }
}
