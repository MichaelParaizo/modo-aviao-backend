package br.com.modoaviao.controller.admin;

import br.com.modoaviao.dto.QuizOpcaoForm;
import br.com.modoaviao.dto.QuizPerguntaForm;
import br.com.modoaviao.dto.QuizResultadoForm;
import br.com.modoaviao.model.ModoFuga;
import br.com.modoaviao.model.QuizOpcao;
import br.com.modoaviao.model.QuizPergunta;
import br.com.modoaviao.model.QuizResultado;
import br.com.modoaviao.repository.QuizOpcaoRepository;
import br.com.modoaviao.repository.QuizPerguntaRepository;
import br.com.modoaviao.repository.QuizResultadoRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AdminQuizController {

    private final QuizPerguntaRepository quizPerguntaRepository;
    private final QuizOpcaoRepository quizOpcaoRepository;
    private final QuizResultadoRepository quizResultadoRepository;

    @GetMapping("/admin/quiz")
    public String listar(Model model) {
        List<QuizPergunta> perguntas = quizPerguntaRepository.findAllByOrderByOrdemAsc();

        Map<Long, Integer> quantidadeOpcoes = new LinkedHashMap<>();
        for (QuizPergunta pergunta : perguntas) {
            quantidadeOpcoes.put(pergunta.getId(), quizOpcaoRepository.findByPerguntaOrderByOrdemAsc(pergunta).size());
        }

        Map<ModoFuga, QuizResultado> resultadosPorModo = new LinkedHashMap<>();
        for (QuizResultado resultado : quizResultadoRepository.findAll()) {
            resultadosPorModo.put(resultado.getModo(), resultado);
        }

        model.addAttribute("perguntas", perguntas);
        model.addAttribute("quantidadeOpcoes", quantidadeOpcoes);
        model.addAttribute("modos", ModoFuga.values());
        model.addAttribute("resultadosPorModo", resultadosPorModo);
        return "admin/quiz-lista";
    }

    @GetMapping("/admin/quiz/pergunta/nova")
    public String novaPergunta(Model model) {
        model.addAttribute("perguntaForm", new QuizPerguntaForm());
        model.addAttribute("modosDisponiveis", ModoFuga.values());
        return "admin/quiz-pergunta-form";
    }

    @GetMapping("/admin/quiz/pergunta/{id}/editar")
    public String editarPergunta(@PathVariable Long id, Model model) {
        QuizPergunta pergunta = quizPerguntaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pergunta nao encontrada: " + id));

        List<QuizOpcao> opcoesExistentes = quizOpcaoRepository.findByPerguntaOrderByOrdemAsc(pergunta);

        QuizPerguntaForm form = new QuizPerguntaForm();
        form.setId(pergunta.getId());
        form.setOrdem(pergunta.getOrdem());
        form.setEnunciado(pergunta.getEnunciado());

        List<QuizOpcaoForm> slots = form.getOpcoes();
        for (int i = 0; i < opcoesExistentes.size() && i < slots.size(); i++) {
            QuizOpcao opcao = opcoesExistentes.get(i);
            QuizOpcaoForm slot = slots.get(i);
            slot.setId(opcao.getId());
            slot.setTexto(opcao.getTexto());
            slot.setModo(opcao.getModo() != null ? opcao.getModo().name() : "");
            slot.setOrdem(opcao.getOrdem());
        }

        model.addAttribute("perguntaForm", form);
        model.addAttribute("modosDisponiveis", ModoFuga.values());
        return "admin/quiz-pergunta-form";
    }

    @PostMapping("/admin/quiz/pergunta/salvar")
    @Transactional
    public String salvarPergunta(@ModelAttribute QuizPerguntaForm form) {
        QuizPergunta pergunta = (form.getId() != null)
                ? quizPerguntaRepository.findById(form.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Pergunta nao encontrada: " + form.getId()))
                : new QuizPergunta();

        pergunta.setOrdem(form.getOrdem());
        pergunta.setEnunciado(form.getEnunciado());
        pergunta = quizPerguntaRepository.save(pergunta);

        salvarOpcoes(pergunta, form.getOpcoes());

        return "redirect:/admin/quiz";
    }

    /**
     * Slots com texto em branco sao ignorados (o admin nao usou aquele
     * slot). Slots preenchidos criam/atualizam a QuizOpcao correspondente
     * (casando pelo id do slot, quando existe). Opcoes que existiam antes
     * mas cujo slot ficou em branco desta vez sao removidas - assim apagar
     * o texto de um slot remove a opcao de verdade, nao deixa lixo.
     */
    private void salvarOpcoes(QuizPergunta pergunta, List<QuizOpcaoForm> slots) {
        List<QuizOpcao> existentes = quizOpcaoRepository.findByPerguntaOrderByOrdemAsc(pergunta);
        Map<Long, QuizOpcao> existentesPorId = new LinkedHashMap<>();
        for (QuizOpcao opcao : existentes) {
            existentesPorId.put(opcao.getId(), opcao);
        }

        Set<Long> idsMantidos = new HashSet<>();

        for (QuizOpcaoForm slot : slots) {
            if (slot.getTexto() == null || slot.getTexto().isBlank()) {
                continue;
            }

            QuizOpcao opcao = (slot.getId() != null && existentesPorId.containsKey(slot.getId()))
                    ? existentesPorId.get(slot.getId())
                    : new QuizOpcao();

            opcao.setPergunta(pergunta);
            opcao.setTexto(slot.getTexto());
            opcao.setModo(parseModo(slot.getModo()));
            opcao.setOrdem(slot.getOrdem());

            QuizOpcao salva = quizOpcaoRepository.save(opcao);
            idsMantidos.add(salva.getId());
        }

        for (QuizOpcao existente : existentes) {
            if (!idsMantidos.contains(existente.getId())) {
                quizOpcaoRepository.delete(existente);
            }
        }
    }

    private ModoFuga parseModo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return ModoFuga.valueOf(valor);
    }

    @PostMapping("/admin/quiz/pergunta/{id}/excluir")
    @Transactional
    public String excluirPergunta(@PathVariable Long id) {
        QuizPergunta pergunta = quizPerguntaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pergunta nao encontrada: " + id));

        quizOpcaoRepository.deleteAll(quizOpcaoRepository.findByPerguntaOrderByOrdemAsc(pergunta));
        quizPerguntaRepository.delete(pergunta);

        return "redirect:/admin/quiz";
    }

    @GetMapping("/admin/quiz/resultado/{modo}/editar")
    public String editarResultado(@PathVariable ModoFuga modo, Model model) {
        QuizResultadoForm form = quizResultadoRepository.findByModo(modo)
                .map(this::paraForm)
                .orElseGet(() -> {
                    QuizResultadoForm novo = new QuizResultadoForm();
                    novo.setModo(modo);
                    return novo;
                });

        model.addAttribute("resultadoForm", form);
        return "admin/quiz-resultado-form";
    }

    @PostMapping("/admin/quiz/resultado/salvar")
    @Transactional
    public String salvarResultado(@ModelAttribute QuizResultadoForm form) {
        QuizResultado resultado = quizResultadoRepository.findByModo(form.getModo())
                .orElseGet(QuizResultado::new);

        resultado.setModo(form.getModo());
        resultado.setTitulo(form.getTitulo());
        resultado.setEmoji(form.getEmoji());
        resultado.setTexto(form.getTexto());

        quizResultadoRepository.save(resultado);

        return "redirect:/admin/quiz";
    }

    private QuizResultadoForm paraForm(QuizResultado resultado) {
        QuizResultadoForm form = new QuizResultadoForm();
        form.setModo(resultado.getModo());
        form.setTitulo(resultado.getTitulo());
        form.setEmoji(resultado.getEmoji());
        form.setTexto(resultado.getTexto());
        return form;
    }
}
