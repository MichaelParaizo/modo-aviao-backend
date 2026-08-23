package br.com.modoaviao.controller;

import br.com.modoaviao.dto.CardSalvoRequest;
import br.com.modoaviao.dto.ProgressSnapshotDto;
import br.com.modoaviao.dto.RespostaCheckpointRequest;
import br.com.modoaviao.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping
    public ProgressSnapshotDto getSnapshot(Authentication authentication) {
        return progressService.getSnapshot(authentication.getName());
    }

    @PostMapping("/chapter/{slug}/complete")
    public ProgressSnapshotDto marcarConcluido(Authentication authentication, @PathVariable String slug) {
        progressService.marcarCapituloConcluido(authentication.getName(), slug);
        return progressService.getSnapshot(authentication.getName());
    }

    @DeleteMapping("/chapter/{slug}/complete")
    public ProgressSnapshotDto desmarcarConcluido(Authentication authentication, @PathVariable String slug) {
        progressService.desmarcarCapituloConcluido(authentication.getName(), slug);
        return progressService.getSnapshot(authentication.getName());
    }

    @PostMapping("/checkpoint/{slug}")
    public ProgressSnapshotDto responderCheckpoint(Authentication authentication, @PathVariable String slug,
            @RequestBody RespostaCheckpointRequest request) {
        progressService.responderCheckpoint(authentication.getName(), slug, request.getNota());
        return progressService.getSnapshot(authentication.getName());
    }

    @PostMapping("/card/{slug}")
    public ProgressSnapshotDto salvarCard(Authentication authentication, @PathVariable String slug,
            @RequestBody CardSalvoRequest request) {
        progressService.salvarCard(authentication.getName(), slug, request.getTitulo(), request.getTexto());
        return progressService.getSnapshot(authentication.getName());
    }

    @DeleteMapping("/card/{slug}")
    public ProgressSnapshotDto removerCard(Authentication authentication, @PathVariable String slug) {
        progressService.removerCard(authentication.getName(), slug);
        return progressService.getSnapshot(authentication.getName());
    }
}
