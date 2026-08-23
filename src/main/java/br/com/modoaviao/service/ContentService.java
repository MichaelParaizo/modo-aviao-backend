package br.com.modoaviao.service;

import br.com.modoaviao.dto.ChapterDto;
import br.com.modoaviao.dto.CheckpointDto;
import br.com.modoaviao.model.Capitulo;
import br.com.modoaviao.model.Checkpoint;
import br.com.modoaviao.repository.CapituloRepository;
import br.com.modoaviao.repository.CheckpointRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final CapituloRepository capituloRepository;
    private final CheckpointRepository checkpointRepository;

    @Transactional(readOnly = true)
    public List<ChapterDto> getChapters() {
        return capituloRepository.findAllByOrderByOrdemAsc().stream()
                .map(this::toChapterDto)
                .toList();
    }

    private ChapterDto toChapterDto(Capitulo capitulo) {
        CheckpointDto checkpointDto = checkpointRepository.findByCapitulo(capitulo)
                .map(this::toCheckpointDto)
                .orElse(null);

        return new ChapterDto(
                capitulo.getSlug(),
                capitulo.getOrdem(),
                capitulo.getParte(),
                capitulo.getTitulo(),
                capitulo.getSubtitulo(),
                capitulo.getConteudoMarkdown(),
                capitulo.getImagemCapa(),
                capitulo.getAudioUrl(),
                checkpointDto);
    }

    private CheckpointDto toCheckpointDto(Checkpoint checkpoint) {
        return new CheckpointDto(
                checkpoint.getEspelhoDeTexto(),
                checkpoint.getPerguntaEscala(),
                checkpoint.getCardDeBolso());
    }
}
