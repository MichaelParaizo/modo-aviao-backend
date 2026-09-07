package br.com.modoaviao.repository;

import br.com.modoaviao.model.Capitulo;
import br.com.modoaviao.model.RespostaCheckpoint;
import br.com.modoaviao.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaCheckpointRepository extends JpaRepository<RespostaCheckpoint, Long> {

    List<RespostaCheckpoint> findByUsuario(Usuario usuario);

    Optional<RespostaCheckpoint> findByUsuarioAndCapitulo(Usuario usuario, Capitulo capitulo);

    void deleteByUsuario(Usuario usuario);
}
