package br.com.modoaviao.repository;

import br.com.modoaviao.model.Capitulo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapituloRepository extends JpaRepository<Capitulo, Long> {

    List<Capitulo> findAllByOrderByOrdemAsc();

    Optional<Capitulo> findBySlug(String slug);
}
