package com.samu.customerservice.customer;

import com.samu.customerservice.customer.dto.CreateCustomerRequest;
import com.samu.customerservice.customer.dto.CustomerResponse;
import com.samu.customerservice.exception.CustomerAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createsCustomerSuccessfully() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Maria Silva");
        request.setCpf("12345678901");
        request.setEmail("maria@example.com");
        request.setStatus(CustomerStatus.ACTIVE);

        Customer savedCustomer = new Customer(
                "Maria Silva", "12345678901", "maria@example.com", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(savedCustomer, "id", 1L);
        when(customerRepository.existsByCpf(request.getCpf())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponse response = customerService.create(request);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).existsByCpf(request.getCpf());
        verify(customerRepository).save(customerCaptor.capture());
        Customer customer = customerCaptor.getValue();
        assertNull(customer.getId());
        assertEquals(request.getName(), customer.getName());
        assertEquals(request.getCpf(), customer.getCpf());
        assertEquals(request.getEmail(), customer.getEmail());
        assertEquals(request.getStatus(), customer.getStatus());

        assertEquals(savedCustomer.getId(), response.getId());
        assertEquals(savedCustomer.getName(), response.getName());
        assertEquals(savedCustomer.getCpf(), response.getCpf());
        assertEquals(savedCustomer.getEmail(), response.getEmail());
        assertEquals(savedCustomer.getStatus(), response.getStatus());
    }

    @Test
    void throwsWhenCpfAlreadyExists() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Maria Silva");
        request.setCpf("12345678901");
        request.setEmail("maria@example.com");
        request.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.existsByCpf(request.getCpf())).thenReturn(true);

        CustomerAlreadyExistsException exception = assertThrows(
                CustomerAlreadyExistsException.class, () -> customerService.create(request));

        assertTrue(exception.getMessage().contains(request.getCpf()));
        verify(customerRepository).existsByCpf(request.getCpf());
        verify(customerRepository, never()).save(any(Customer.class));
    }
}
