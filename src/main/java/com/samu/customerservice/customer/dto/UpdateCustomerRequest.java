package com.samu.customerservice.customer.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.samu.customerservice.customer.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateCustomerRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private CustomerStatus status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String propertyName, Object value) {
        if ("cpf".equals(propertyName)) {
            throw new IllegalArgumentException("CPF nao pode ser alterado.");
        }

        throw new IllegalArgumentException("Campo '" + propertyName + "' nao e permitido.");
    }
}
