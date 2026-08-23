package br.com.modoaviao.controller;

import br.com.modoaviao.dto.QuizPerguntaPublicaDto;
import br.com.modoaviao.dto.QuizResultadoDto;
import br.com.modoaviao.dto.QuizSubmitRequest;
import br.com.modoaviao.service.QuizService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public List<QuizPerguntaPublicaDto> getQuiz() {
        return quizService.getQuiz();
    }

    @PostMapping("/submit")
    public QuizResultadoDto submit(@RequestBody QuizSubmitRequest request) {
        return quizService.calcularResultado(request.getOpcaoIds());
    }
}
