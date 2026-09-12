package com.samu.customerservice.exception;

public class ScoreServiceUnavailableException extends RuntimeException {

    public ScoreServiceUnavailableException(String cpf) {
        super("Servico de Score indisponivel para o CPF " + cpf + ".");
    }
}
