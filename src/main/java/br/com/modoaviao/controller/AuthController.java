package br.com.modoaviao.controller;

import br.com.modoaviao.dto.AuthResponse;
import br.com.modoaviao.dto.LoginRequest;
import br.com.modoaviao.dto.SignupRequest;
import br.com.modoaviao.service.AuthService;
import jakarta.validation.Valid;
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
}
