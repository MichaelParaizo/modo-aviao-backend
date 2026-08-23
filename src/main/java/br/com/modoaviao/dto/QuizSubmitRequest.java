package br.com.modoaviao.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizSubmitRequest {

    private List<Long> opcaoIds;
}
