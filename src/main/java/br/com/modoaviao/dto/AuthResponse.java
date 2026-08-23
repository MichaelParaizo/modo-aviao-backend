package br.com.modoaviao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long id;
    private String nome;
    private String email;
    private String role;
}
