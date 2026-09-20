package br.com.modoaviao.exception;

public class PagamentoIndisponivelException extends RuntimeException {

    public PagamentoIndisponivelException(String message) {
        super(message);
    }

    public PagamentoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
