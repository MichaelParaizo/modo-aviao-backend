package br.com.modoaviao.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizResultadoDto {

    private String modoDominante;
    private String tituloDominante;
    private String emojiDominante;
    private String textoDominante;
    private String modoSecundario;
    private String tituloSecundario;
    private Map<String, Integer> pontuacao;
}
