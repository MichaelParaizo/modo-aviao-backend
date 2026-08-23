package br.com.modoaviao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {

    private Long id;
    private String nome;
    private String email;
    private String whatsapp;
    private boolean acessoLiberado;
}
