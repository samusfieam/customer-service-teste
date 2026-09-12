package com.samu.customerservice.customer.dto;

import com.samu.customerservice.customer.CustomerStatus;

public class CustomerResponse {

    private Long id;
    private String name;
    private String cpf;
    private String email;
    private CustomerStatus status;

    public CustomerResponse() {
    }

    public CustomerResponse(Long id, String name, String cpf, String email, CustomerStatus status) {
        this.id = id;
        this.name = name;
        this.cpf = cpf;
        this.email = email;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCpf() {
        return cpf;
    }

    public String getEmail() {
        return email;
    }

    public CustomerStatus getStatus() {
        return status;
    }
}
