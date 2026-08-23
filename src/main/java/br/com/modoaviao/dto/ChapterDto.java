package br.com.modoaviao.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChapterDto {

    private String id;
    private int ordem;
    private String parte;
    private String titulo;
    private String subtitulo;
    private String conteudoMarkdown;
    private String imagemCapa;
    private String audioUrl;
    private CheckpointDto checkpoint;
}
