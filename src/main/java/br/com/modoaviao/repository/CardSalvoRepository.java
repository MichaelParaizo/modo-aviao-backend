package br.com.modoaviao.repository;

import br.com.modoaviao.model.CardSalvo;
import br.com.modoaviao.model.Capitulo;
import br.com.modoaviao.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSalvoRepository extends JpaRepository<CardSalvo, Long> {

    List<CardSalvo> findByUsuario(Usuario usuario);

    Optional<CardSalvo> findByUsuarioAndCapitulo(Usuario usuario, Capitulo capitulo);

    void deleteByUsuarioAndCapitulo(Usuario usuario, Capitulo capitulo);

    void deleteByUsuario(Usuario usuario);
}
