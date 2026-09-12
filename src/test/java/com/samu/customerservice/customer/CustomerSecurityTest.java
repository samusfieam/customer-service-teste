package com.samu.customerservice.customer;

import com.samu.customerservice.config.OpenApiConfig;
import com.samu.customerservice.customer.dto.CreateCustomerRequest;
import com.samu.customerservice.customer.dto.CustomerResponse;
import com.samu.customerservice.exception.GlobalExceptionHandler;
import com.samu.customerservice.score.ScoreClient;
import com.samu.customerservice.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class})
class CustomerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private ScoreClient scoreClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void returnsUnauthorizedWhenGetCustomersHasNoToken() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsGetCustomersWithUserToken() throws Exception {
        when(jwtDecoder.decode("user-token")).thenReturn(jwtWithRoles("user-token", "USER"));
        when(customerService.findAll(null)).thenReturn(List.of());

        mockMvc.perform(get("/customers")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk());

        verify(customerService).findAll(null);
    }

    @Test
    void forbidsPostCustomerWithUserToken() throws Exception {
        when(jwtDecoder.decode("user-token")).thenReturn(jwtWithRoles("user-token", "USER"));

        String requestBody = """
                {
                  "name": "Maria Silva",
                  "cpf": "12345678901",
                  "email": "maria@example.com",
                  "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        verify(customerService, never()).create(any(CreateCustomerRequest.class));
    }

    @Test
    void allowsPostCustomerWithAdminToken() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwtWithRoles("admin-token", "ADMIN"));
        when(customerService.create(any(CreateCustomerRequest.class)))
                .thenReturn(new CustomerResponse(
                        1L,
                        "Maria Silva",
                        "12345678901",
                        "maria@example.com",
                        CustomerStatus.ACTIVE));

        String requestBody = """
                {
                  "name": "Maria Silva",
                  "cpf": "12345678901",
                  "email": "maria@example.com",
                  "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/customers")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        verify(customerService).create(any(CreateCustomerRequest.class));
    }

    private Jwt jwtWithRoles(String tokenValue, String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer("http://localhost:8082/realms/customer-service")
                .subject(role.toLowerCase())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
