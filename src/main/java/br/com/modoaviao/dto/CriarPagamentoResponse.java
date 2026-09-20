package br.com.modoaviao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CriarPagamentoResponse {

    private String checkoutUrl;
    private String externalReference;
}
