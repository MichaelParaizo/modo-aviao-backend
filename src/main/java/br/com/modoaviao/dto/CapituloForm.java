package br.com.modoaviao.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapituloForm {

    private Long id;
    private String slug;
    private int ordem;
    private String parte;
    private String titulo;
    private String subtitulo;
    private String conteudoMarkdown;
    private String imagemCapa;
    private String audioUrl;

    private String espelhoDeTexto;
    private String perguntaEscala;
    private String cardDeBolso;
}
