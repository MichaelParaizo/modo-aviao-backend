package br.com.modoaviao.controller;

import br.com.modoaviao.dto.ChapterDto;
import br.com.modoaviao.service.ContentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/chapters")
    public List<ChapterDto> getChapters() {
        return contentService.getChapters();
    }
}
