package br.com.modoaviao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizOpcaoPublicaDto {

    private Long id;
    private String texto;
    private int ordem;
}
