package br.com.modoaviao.dto;

import br.com.modoaviao.model.ModoFuga;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizResultadoForm {

    private ModoFuga modo;
    private String titulo;
    private String emoji;
    private String texto;
}
