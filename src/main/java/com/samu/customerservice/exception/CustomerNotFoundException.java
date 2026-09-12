package com.samu.customerservice.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long customerId) {
        super("Cliente com ID " + customerId + " nao foi encontrado.");
    }
}
