package br.com.modoaviao.controller;

import br.com.modoaviao.dto.AuthResponse;
import br.com.modoaviao.dto.ForgotPasswordRequest;
import br.com.modoaviao.dto.LoginRequest;
import br.com.modoaviao.dto.ResetPasswordRequest;
import br.com.modoaviao.dto.SignupRequest;
import br.com.modoaviao.service.AuthService;
import br.com.modoaviao.service.PasswordResetService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Nao ha mais estado de sessao/cookie no servidor para invalidar - o
    // token e stateless (JWT) e a responsabilidade de "deslogar" e do
    // front, removendo o token guardado (ex: localStorage). Mantido como
    // endpoint simples para o front ter algo para chamar nesse fluxo.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Se este email tiver conta, você receberá um código."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getEmail(), request.getCode(), request.getNovaSenha());
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
    }
}
