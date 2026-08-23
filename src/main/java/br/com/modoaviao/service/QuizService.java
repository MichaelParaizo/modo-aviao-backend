package br.com.modoaviao.service;

import br.com.modoaviao.dto.QuizOpcaoPublicaDto;
import br.com.modoaviao.dto.QuizPerguntaPublicaDto;
import br.com.modoaviao.dto.QuizResultadoDto;
import br.com.modoaviao.exception.QuizRespostaInvalidaException;
import br.com.modoaviao.exception.QuizResultadoNaoEncontradoException;
import br.com.modoaviao.model.ModoFuga;
import br.com.modoaviao.model.QuizOpcao;
import br.com.modoaviao.model.QuizPergunta;
import br.com.modoaviao.model.QuizResultado;
import br.com.modoaviao.repository.QuizOpcaoRepository;
import br.com.modoaviao.repository.QuizPerguntaRepository;
import br.com.modoaviao.repository.QuizResultadoRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizPerguntaRepository quizPerguntaRepository;
    private final QuizOpcaoRepository quizOpcaoRepository;
    private final QuizResultadoRepository quizResultadoRepository;

    @Transactional(readOnly = true)
    public List<QuizPerguntaPublicaDto> getQuiz() {
        return quizPerguntaRepository.findAllByOrderByOrdemAsc().stream()
                .map(this::toPerguntaPublicaDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResultadoDto calcularResultado(List<Long> opcaoIds) {
        List<QuizOpcao> opcoesEscolhidas = (opcaoIds == null || opcaoIds.isEmpty())
                ? List.of()
                : quizOpcaoRepository.findAllById(opcaoIds);

        Map<ModoFuga, Integer> contagem = new LinkedHashMap<>();
        for (ModoFuga modo : ModoFuga.values()) {
            contagem.put(modo, 0);
        }
        for (QuizOpcao opcao : opcoesEscolhidas) {
            if (opcao.getModo() != null) {
                contagem.merge(opcao.getModo(), 1, Integer::sum);
            }
        }

        int totalPontuado = contagem.values().stream().mapToInt(Integer::intValue).sum();
        if (totalPontuado == 0) {
            throw new QuizRespostaInvalidaException("Nenhuma resposta válida enviada");
        }

        ModoFuga dominante = escolherMaior(contagem, null);
        ModoFuga secundario = escolherMaior(contagem, dominante);
        if (secundario != null && contagem.get(secundario) == 0) {
            secundario = null;
        }

        QuizResultado resultadoDominante = buscarResultado(dominante);
        String tituloSecundario = (secundario != null) ? buscarResultado(secundario).getTitulo() : null;

        Map<String, Integer> pontuacao = new LinkedHashMap<>();
        for (Map.Entry<ModoFuga, Integer> entry : contagem.entrySet()) {
            pontuacao.put(entry.getKey().name(), entry.getValue());
        }

        return new QuizResultadoDto(
                dominante.name(),
                resultadoDominante.getTitulo(),
                resultadoDominante.getEmoji(),
                resultadoDominante.getTexto(),
                secundario != null ? secundario.name() : null,
                tituloSecundario,
                pontuacao);
    }

    /**
     * Percorre os modos na ordem de declaracao do enum (FANTASMA, SABOTADOR,
     * IDEALIZADOR, COLECIONADOR), que e tambem a ordem de prioridade de
     * desempate pedida. So troca o "escolhido" quando encontra uma contagem
     * ESTRITAMENTE maior - por isso, em caso de empate, o primeiro na ordem
     * de prioridade permanece vencedor. Usado tanto para achar o dominante
     * (excluir = null) quanto o secundario (excluir = o dominante ja achado).
     */
    private ModoFuga escolherMaior(Map<ModoFuga, Integer> contagem, ModoFuga excluir) {
        ModoFuga escolhido = null;
        int maior = -1;
        for (ModoFuga modo : ModoFuga.values()) {
            if (modo == excluir) {
                continue;
            }
            int valor = contagem.get(modo);
            if (valor > maior) {
                maior = valor;
                escolhido = modo;
            }
        }
        return escolhido;
    }

    private QuizResultado buscarResultado(ModoFuga modo) {
        return quizResultadoRepository.findByModo(modo)
                .orElseThrow(() -> new QuizResultadoNaoEncontradoException(
                        "Resultado do quiz não configurado para o modo: " + modo));
    }

    private QuizPerguntaPublicaDto toPerguntaPublicaDto(QuizPergunta pergunta) {
        List<QuizOpcaoPublicaDto> opcoes = quizOpcaoRepository.findByPerguntaOrderByOrdemAsc(pergunta).stream()
                .map(opcao -> new QuizOpcaoPublicaDto(opcao.getId(), opcao.getTexto(), opcao.getOrdem()))
                .toList();

        return new QuizPerguntaPublicaDto(pergunta.getId(), pergunta.getOrdem(), pergunta.getEnunciado(), opcoes);
    }
}
