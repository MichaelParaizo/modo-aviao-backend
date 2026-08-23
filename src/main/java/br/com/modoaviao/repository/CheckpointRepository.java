package br.com.modoaviao.repository;

import br.com.modoaviao.model.Capitulo;
import br.com.modoaviao.model.Checkpoint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckpointRepository extends JpaRepository<Checkpoint, Long> {

    Optional<Checkpoint> findByCapitulo(Capitulo capitulo);
}
