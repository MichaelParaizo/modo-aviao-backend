package br.com.modoaviao.service;

import br.com.modoaviao.dto.PocketCardDto;
import br.com.modoaviao.dto.ProgressSnapshotDto;
import br.com.modoaviao.exception.CapituloNaoEncontradoException;
import br.com.modoaviao.exception.NotaInvalidaException;
import br.com.modoaviao.exception.UsuarioNaoEncontradoException;
import br.com.modoaviao.model.CardSalvo;
import br.com.modoaviao.model.Capitulo;
import br.com.modoaviao.model.ProgressoCapitulo;
import br.com.modoaviao.model.RespostaCheckpoint;
import br.com.modoaviao.model.Usuario;
import br.com.modoaviao.repository.CapituloRepository;
import br.com.modoaviao.repository.CardSalvoRepository;
import br.com.modoaviao.repository.ProgressoCapituloRepository;
import br.com.modoaviao.repository.RespostaCheckpointRepository;
import br.com.modoaviao.repository.UsuarioRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UsuarioRepository usuarioRepository;
    private final CapituloRepository capituloRepository;
    private final ProgressoCapituloRepository progressoCapituloRepository;
    private final RespostaCheckpointRepository respostaCheckpointRepository;
    private final CardSalvoRepository cardSalvoRepository;

    @Transactional(readOnly = true)
    public ProgressSnapshotDto getSnapshot(String email) {
        Usuario usuario = buscarUsuario(email);

        List<String> capitulosConcluidos = progressoCapituloRepository.findByUsuario(usuario).stream()
                .map(progresso -> progresso.getCapitulo().getSlug())
                .toList();

        Map<String, Integer> respostas = new LinkedHashMap<>();
        for (RespostaCheckpoint resposta : respostaCheckpointRepository.findByUsuario(usuario)) {
            respostas.put(resposta.getCapitulo().getSlug(), resposta.getNota());
        }

        List<PocketCardDto> cardsSalvos = cardSalvoRepository.findByUsuario(usuario).stream()
                .map(card -> new PocketCardDto(card.getCapitulo().getSlug(), card.getTitulo(), card.getTexto()))
                .toList();

        return new ProgressSnapshotDto(capitulosConcluidos, respostas, cardsSalvos);
    }

    @Transactional
    public void marcarCapituloConcluido(String email, String slug) {
        Usuario usuario = buscarUsuario(email);
        Capitulo capitulo = buscarCapitulo(slug);

        if (progressoCapituloRepository.findByUsuarioAndCapitulo(usuario, capitulo).isPresent()) {
            return;
        }

        ProgressoCapitulo progresso = new ProgressoCapitulo();
        progresso.setUsuario(usuario);
        progresso.setCapitulo(capitulo);
        progressoCapituloRepository.save(progresso);
    }

    @Transactional
    public void desmarcarCapituloConcluido(String email, String slug) {
        Usuario usuario = buscarUsuario(email);
        Capitulo capitulo = buscarCapitulo(slug);

        progressoCapituloRepository.findByUsuarioAndCapitulo(usuario, capitulo)
                .ifPresent(progressoCapituloRepository::delete);
    }

    @Transactional
    public void responderCheckpoint(String email, String slug, int nota) {
        if (nota < 1 || nota > 5) {
            throw new NotaInvalidaException("A nota deve estar entre 1 e 5");
        }

        Usuario usuario = buscarUsuario(email);
        Capitulo capitulo = buscarCapitulo(slug);

        RespostaCheckpoint resposta = respostaCheckpointRepository.findByUsuarioAndCapitulo(usuario, capitulo)
                .orElseGet(RespostaCheckpoint::new);
        resposta.setUsuario(usuario);
        resposta.setCapitulo(capitulo);
        resposta.setNota(nota);
        respostaCheckpointRepository.save(resposta);
    }

    @Transactional
    public void salvarCard(String email, String slug, String titulo, String texto) {
        Usuario usuario = buscarUsuario(email);
        Capitulo capitulo = buscarCapitulo(slug);

        CardSalvo card = cardSalvoRepository.findByUsuarioAndCapitulo(usuario, capitulo)
                .orElseGet(CardSalvo::new);
        card.setUsuario(usuario);
        card.setCapitulo(capitulo);
        card.setTitulo(titulo);
        card.setTexto(texto);
        cardSalvoRepository.save(card);
    }

    @Transactional
    public void removerCard(String email, String slug) {
        Usuario usuario = buscarUsuario(email);
        Capitulo capitulo = buscarCapitulo(slug);

        cardSalvoRepository.findByUsuarioAndCapitulo(usuario, capitulo)
                .ifPresent(cardSalvoRepository::delete);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuario autenticado nao encontrado: " + email));
    }

    private Capitulo buscarCapitulo(String slug) {
        return capituloRepository.findBySlug(slug)
                .orElseThrow(() -> new CapituloNaoEncontradoException("Capítulo não encontrado: " + slug));
    }
}
