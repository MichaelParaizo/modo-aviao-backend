package br.com.modoaviao.repository;

import br.com.modoaviao.model.QuizOpcao;
import br.com.modoaviao.model.QuizPergunta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizOpcaoRepository extends JpaRepository<QuizOpcao, Long> {

    List<QuizOpcao> findByPerguntaOrderByOrdemAsc(QuizPergunta pergunta);
}
