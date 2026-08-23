package br.com.modoaviao.repository;

import br.com.modoaviao.model.ModoFuga;
import br.com.modoaviao.model.QuizResultado;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizResultadoRepository extends JpaRepository<QuizResultado, Long> {

    Optional<QuizResultado> findByModo(ModoFuga modo);
}
