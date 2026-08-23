package br.com.modoaviao.controller.admin;

import br.com.modoaviao.dto.CapituloForm;
import br.com.modoaviao.model.Capitulo;
import br.com.modoaviao.model.Checkpoint;
import br.com.modoaviao.repository.CapituloRepository;
import br.com.modoaviao.repository.CheckpointRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class AdminCapituloController {

    private final CapituloRepository capituloRepository;
    private final CheckpointRepository checkpointRepository;

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String listar(Model model) {
        List<Capitulo> capitulos = capituloRepository.findAllByOrderByOrdemAsc();

        Map<Long, Boolean> temCheckpoint = new LinkedHashMap<>();
        for (Capitulo capitulo : capitulos) {
            temCheckpoint.put(capitulo.getId(), checkpointRepository.findByCapitulo(capitulo).isPresent());
        }

        model.addAttribute("capitulos", capitulos);
        model.addAttribute("temCheckpoint", temCheckpoint);
        return "admin/capitulos-lista";
    }

    @GetMapping("/admin/capitulos/novo")
    public String novo(Model model) {
        model.addAttribute("capituloForm", new CapituloForm());
        return "admin/capitulo-form";
    }

    @GetMapping("/admin/capitulos/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Capitulo capitulo = capituloRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Capitulo nao encontrado: " + id));

        CapituloForm form = new CapituloForm();
        form.setId(capitulo.getId());
        form.setSlug(capitulo.getSlug());
        form.setOrdem(capitulo.getOrdem());
        form.setParte(capitulo.getParte());
        form.setTitulo(capitulo.getTitulo());
        form.setSubtitulo(capitulo.getSubtitulo());
        form.setConteudoMarkdown(capitulo.getConteudoMarkdown());
        form.setImagemCapa(capitulo.getImagemCapa());
        form.setAudioUrl(capitulo.getAudioUrl());

        checkpointRepository.findByCapitulo(capitulo).ifPresent(checkpoint -> {
            form.setEspelhoDeTexto(checkpoint.getEspelhoDeTexto());
            form.setPerguntaEscala(checkpoint.getPerguntaEscala());
            form.setCardDeBolso(checkpoint.getCardDeBolso());
        });

        model.addAttribute("capituloForm", form);
        return "admin/capitulo-form";
    }

    @PostMapping("/admin/capitulos/salvar")
    @Transactional
    public String salvar(@ModelAttribute CapituloForm form, Model model) {
        int camposPreenchidos = contarCamposPreenchidos(form);

        // Checkpoint e tudo-ou-nada: o app espera os 3 campos sempre
        // preenchidos, entao 1 ou 2 preenchidos e um estado invalido que
        // bloqueia o salvamento inteiro (capitulo incluso), para o admin
        // corrigir antes de gravar qualquer coisa.
        if (camposPreenchidos != 0 && camposPreenchidos != 3) {
            model.addAttribute("capituloForm", form);
            model.addAttribute("erroCheckpoint",
                    "O checkpoint precisa ter os três campos preenchidos, ou todos vazios. "
                            + "Preencha o Espelho de Texto, a Pergunta de Escala e o Card de Bolso — "
                            + "ou apague os três para salvar o capítulo sem checkpoint.");
            return "admin/capitulo-form";
        }

        Capitulo capitulo = (form.getId() != null)
                ? capituloRepository.findById(form.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Capitulo nao encontrado: " + form.getId()))
                : new Capitulo();

        capitulo.setSlug(form.getSlug());
        capitulo.setOrdem(form.getOrdem());
        capitulo.setParte(form.getParte());
        capitulo.setTitulo(form.getTitulo());
        capitulo.setSubtitulo(form.getSubtitulo());
        capitulo.setConteudoMarkdown(form.getConteudoMarkdown());
        capitulo.setImagemCapa(form.getImagemCapa());
        capitulo.setAudioUrl(form.getAudioUrl());

        capitulo = capituloRepository.save(capitulo);

        salvarOuRemoverCheckpoint(capitulo, form, camposPreenchidos);

        return "redirect:/admin";
    }

    /**
     * camposPreenchidos so chega aqui como 0 ou 3 (a validacao tudo-ou-nada
     * ja barrou qualquer valor parcial antes de chamar este metodo).
     */
    private void salvarOuRemoverCheckpoint(Capitulo capitulo, CapituloForm form, int camposPreenchidos) {
        Optional<Checkpoint> existente = checkpointRepository.findByCapitulo(capitulo);

        if (camposPreenchidos == 0) {
            existente.ifPresent(checkpointRepository::delete);
            return;
        }

        Checkpoint checkpoint = existente.orElseGet(Checkpoint::new);
        checkpoint.setCapitulo(capitulo);
        checkpoint.setEspelhoDeTexto(form.getEspelhoDeTexto());
        checkpoint.setPerguntaEscala(form.getPerguntaEscala());
        checkpoint.setCardDeBolso(form.getCardDeBolso());
        checkpointRepository.save(checkpoint);
    }

    private int contarCamposPreenchidos(CapituloForm form) {
        int count = 0;
        if (temTexto(form.getEspelhoDeTexto())) {
            count++;
        }
        if (temTexto(form.getPerguntaEscala())) {
            count++;
        }
        if (temTexto(form.getCardDeBolso())) {
            count++;
        }
        return count;
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
