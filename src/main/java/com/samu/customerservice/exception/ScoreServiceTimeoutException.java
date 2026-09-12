package com.samu.customerservice.exception;

public class ScoreServiceTimeoutException extends RuntimeException {

    public ScoreServiceTimeoutException(String cpf) {
        super("Tempo limite excedido ao consultar o Score para o CPF " + cpf + ".");
    }
}
