package com.samu.customerservice.exception;

public class CustomerAlreadyExistsException extends RuntimeException {

    public CustomerAlreadyExistsException(String cpf) {
        super("Cliente com CPF " + cpf + " já está cadastrado.");
    }
}
