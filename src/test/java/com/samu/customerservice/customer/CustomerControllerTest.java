package com.samu.customerservice.customer;

import com.samu.customerservice.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest {

    @Test
    void returnsBadRequestWhenPutBodyContainsCpf() throws Exception {
        CustomerService customerService = mock(CustomerService.class);
        CustomerController customerController = new CustomerController(customerService);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(customerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = """
                {
                  "name": "Joao Santos",
                  "cpf": "12345678901",
                  "email": "joao.santos@example.com",
                  "status": "ACTIVE"
                }
                """;

        mockMvc.perform(put("/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("CPF nao pode ser alterado")));

        verifyNoInteractions(customerService);
    }
}
