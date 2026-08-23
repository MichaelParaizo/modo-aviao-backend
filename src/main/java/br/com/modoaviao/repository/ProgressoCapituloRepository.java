package br.com.modoaviao.repository;

import br.com.modoaviao.model.Capitulo;
import br.com.modoaviao.model.ProgressoCapitulo;
import br.com.modoaviao.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressoCapituloRepository extends JpaRepository<ProgressoCapitulo, Long> {

    List<ProgressoCapitulo> findByUsuario(Usuario usuario);

    Optional<ProgressoCapitulo> findByUsuarioAndCapitulo(Usuario usuario, Capitulo capitulo);
}
