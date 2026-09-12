package com.samu.customerservice.customer.dto;

import com.samu.customerservice.customer.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateCustomerRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Pattern(regexp = "[0-9]{11}")
    private String cpf;

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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
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
}
