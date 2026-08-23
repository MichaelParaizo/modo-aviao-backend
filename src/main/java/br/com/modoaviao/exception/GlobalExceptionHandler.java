package br.com.modoaviao.exception;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ponto unico de tratamento de erros da API, para nao repetir esse
 * "traducao de excecao para JSON" em cada controller.
 *
 * Todo handler aqui devolve a resposta via ResponseEntity (nunca deixando o
 * Spring cair no response.sendError() padrao dele) de proposito: sendError()
 * dispara um dispatch de erro que reprocessa a requisicao pela cadeia de
 * seguranca inteira de novo, e o JwtAuthenticationFilter e pulado nesse
 * segundo passe - isso faz qualquer erro (404, 400 de validacao, etc.)
 * virar um 401 enganoso. Mesmo cuidado tomado no rewire-backend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<Map<String, String>> handleEmailJaCadastrado(EmailJaCadastradoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CapituloNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleCapituloNaoEncontrado(CapituloNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NotaInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleNotaInvalida(NotaInvalidaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(QuizRespostaInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleQuizRespostaInvalida(QuizRespostaInvalidaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(QuizResultadoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleQuizResultadoNaoEncontrado(QuizResultadoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    // O usuario do token nao existe mais no banco (ex: deletado enquanto o
    // token ainda era valido). O front deve tratar isso pedindo novo login,
    // nao como um erro interno opaco.
    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioNaoEncontrado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Sessão inválida. Faça login novamente."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email ou senha incorretos"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", mensagem));
    }

    // Corpo da requisicao ilegivel/nao conversivel (JSON invalido). Sem esse
    // handler, o Spring cai no comportamento padrao dele (sendError), que
    // aciona o mesmo bug de error-dispatch descrito acima e vira um 401
    // enganoso em vez do 400 real.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Corpo da requisição inválido ou ilegível"));
    }
}
