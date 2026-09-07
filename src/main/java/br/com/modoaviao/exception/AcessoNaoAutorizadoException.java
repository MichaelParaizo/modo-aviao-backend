package br.com.modoaviao.exception;

public class AcessoNaoAutorizadoException extends RuntimeException {

    public AcessoNaoAutorizadoException(String message) {
        super(message);
    }
}
