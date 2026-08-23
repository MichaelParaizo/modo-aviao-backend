package br.com.modoaviao.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgressSnapshotDto {

    private List<String> capitulosConcluidos;
    private Map<String, Integer> respostas;
    private List<PocketCardDto> cardsSalvos;
}
