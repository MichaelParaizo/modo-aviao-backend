package br.com.modoaviao.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizPerguntaForm {

    public static final int QUANTIDADE_SLOTS_OPCOES = 5;

    private Long id;
    private int ordem;
    private String enunciado;
    private List<QuizOpcaoForm> opcoes = new ArrayList<>();

    /**
     * Pre-preenche 5 slots vazios (com ordem sugerida 1..5) para o
     * formulario sempre renderizar o mesmo numero fixo de blocos de opcao,
     * independente de quantas opcoes a pergunta ja tem.
     */
    public QuizPerguntaForm() {
        for (int i = 0; i < QUANTIDADE_SLOTS_OPCOES; i++) {
            QuizOpcaoForm slot = new QuizOpcaoForm();
            slot.setOrdem(i + 1);
            opcoes.add(slot);
        }
    }
}
