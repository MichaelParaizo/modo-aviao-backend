package br.com.modoaviao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizOpcaoForm {

    private Long id;
    private String texto;

    /**
     * String (nao ModoFuga) de proposito: precisa aceitar "" para
     * representar "nenhum modo / opcao-ancora", e o enum nao tem esse
     * valor. "" e convertido para null ao salvar.
     */
    private String modo;

    private int ordem;
}
