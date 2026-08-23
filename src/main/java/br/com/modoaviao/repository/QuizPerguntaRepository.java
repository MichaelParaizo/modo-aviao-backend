package br.com.modoaviao.repository;

import br.com.modoaviao.model.QuizPergunta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizPerguntaRepository extends JpaRepository<QuizPergunta, Long> {

    List<QuizPergunta> findAllByOrderByOrdemAsc();
}
