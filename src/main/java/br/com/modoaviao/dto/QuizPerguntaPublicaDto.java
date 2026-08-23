package br.com.modoaviao.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizPerguntaPublicaDto {

    private Long id;
    private int ordem;
    private String enunciado;
    private List<QuizOpcaoPublicaDto> opcoes;
}
