package com.samu.customerservice.customer;

import com.samu.customerservice.customer.dto.CreateCustomerRequest;
import com.samu.customerservice.customer.dto.CustomerResponse;
import com.samu.customerservice.customer.dto.UpdateCustomerRequest;
import com.samu.customerservice.customer.event.CustomerCreatedEvent;
import com.samu.customerservice.customer.event.CustomerCreatedEventPublisher;
import com.samu.customerservice.exception.CustomerAlreadyExistsException;
import com.samu.customerservice.exception.CustomerNotFoundException;
import com.samu.customerservice.score.ScoreClient;
import com.samu.customerservice.score.ScoreResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Mock
    private ScoreClient scoreClient;

    @Mock
    private CustomerCreatedEventPublisher customerCreatedEventPublisher;

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

        ArgumentCaptor<CustomerCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CustomerCreatedEvent.class);
        verify(customerCreatedEventPublisher).publish(eventCaptor.capture());
        CustomerCreatedEvent event = eventCaptor.getValue();
        assertNotNull(event.getEventId());
        assertEquals("CUSTOMER_CREATED", event.getEventType());
        assertEquals(savedCustomer.getId(), event.getCustomerId());
        assertNotNull(event.getCreatedAt());
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

    @Test
    void returnsCustomerWhenFindByIdExists() {
        Customer customer = new Customer(
                "Joao Silva", "12345678901", "joao@example.com", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.findById(1L);

        assertEquals(customer.getId(), response.getId());
        assertEquals(customer.getName(), response.getName());
        assertEquals(customer.getCpf(), response.getCpf());
        assertEquals(customer.getEmail(), response.getEmail());
        assertEquals(customer.getStatus(), response.getStatus());
        verify(customerRepository).findById(1L);
    }

    @Test
    void throwsWhenFindByIdDoesNotExist() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class, () -> customerService.findById(1L));

        assertTrue(exception.getMessage().contains("1"));
        verify(customerRepository).findById(1L);
    }

    @Test
    void returnsAllCustomersWhenStatusIsNull() {
        Customer firstCustomer = new Customer(
                "Joao Silva", "12345678901", "joao@example.com", CustomerStatus.ACTIVE);
        Customer secondCustomer = new Customer(
                "Maria Silva", "10987654321", "maria@example.com", CustomerStatus.INACTIVE);
        ReflectionTestUtils.setField(firstCustomer, "id", 1L);
        ReflectionTestUtils.setField(secondCustomer, "id", 2L);
        when(customerRepository.findAll()).thenReturn(List.of(firstCustomer, secondCustomer));

        List<CustomerResponse> responses = customerService.findAll(null);

        assertEquals(2, responses.size());
        assertEquals(firstCustomer.getId(), responses.get(0).getId());
        assertEquals(firstCustomer.getName(), responses.get(0).getName());
        assertEquals(firstCustomer.getCpf(), responses.get(0).getCpf());
        assertEquals(firstCustomer.getEmail(), responses.get(0).getEmail());
        assertEquals(firstCustomer.getStatus(), responses.get(0).getStatus());
        assertEquals(secondCustomer.getId(), responses.get(1).getId());
        assertEquals(secondCustomer.getName(), responses.get(1).getName());
        assertEquals(secondCustomer.getCpf(), responses.get(1).getCpf());
        assertEquals(secondCustomer.getEmail(), responses.get(1).getEmail());
        assertEquals(secondCustomer.getStatus(), responses.get(1).getStatus());
        verify(customerRepository).findAll();
    }

    @Test
    void returnsCustomersFilteredByStatus() {
        Customer customer = new Customer(
                "Joao Silva", "12345678901", "joao@example.com", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepository.findByStatus(CustomerStatus.ACTIVE)).thenReturn(List.of(customer));

        List<CustomerResponse> responses = customerService.findAll(CustomerStatus.ACTIVE);

        assertEquals(1, responses.size());
        assertEquals(customer.getId(), responses.get(0).getId());
        assertEquals(customer.getName(), responses.get(0).getName());
        assertEquals(customer.getCpf(), responses.get(0).getCpf());
        assertEquals(customer.getEmail(), responses.get(0).getEmail());
        assertEquals(customer.getStatus(), responses.get(0).getStatus());
        verify(customerRepository).findByStatus(CustomerStatus.ACTIVE);
    }

    @Test
    void returnsCustomersFoundByName() {
        Customer customer = new Customer(
                "Joao Silva", "12345678901", "joao@example.com", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepository.findByNameContainingIgnoreCase("joao")).thenReturn(List.of(customer));

        List<CustomerResponse> responses = customerService.searchByName("joao");

        assertEquals(1, responses.size());
        assertEquals(customer.getId(), responses.get(0).getId());
        assertEquals(customer.getName(), responses.get(0).getName());
        assertEquals(customer.getCpf(), responses.get(0).getCpf());
        assertEquals(customer.getEmail(), responses.get(0).getEmail());
        assertEquals(customer.getStatus(), responses.get(0).getStatus());
        verify(customerRepository).findByNameContainingIgnoreCase("joao");
    }

    @Test
    void updatesCustomerSuccessfully() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setName("Joao Santos");
        request.setEmail("joao.santos@example.com");
        request.setStatus(CustomerStatus.INACTIVE);

        Customer customer = new Customer(
                "Joao Silva", "12345678901", "joao@example.com", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerResponse response = customerService.update(1L, request);

        assertEquals(1L, response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals("12345678901", response.getCpf());
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals(request.getStatus(), response.getStatus());
        assertEquals(request.getName(), customer.getName());
        assertEquals("12345678901", customer.getCpf());
        assertEquals(request.getEmail(), customer.getEmail());
        assertEquals(request.getStatus(), customer.getStatus());
        verify(customerRepository).findById(1L);
        verify(customerRepository).save(customer);
    }

    @Test
    void throwsWhenUpdateCustomerDoesNotExist() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setName("Joao Santos");
        request.setEmail("joao.santos@example.com");
        request.setStatus(CustomerStatus.INACTIVE);
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class, () -> customerService.update(1L, request));

        assertTrue(exception.getMessage().contains("1"));
        verify(customerRepository).findById(1L);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void deletesExistingCustomer() {
        Customer customer = new Customer(
                "Joao Silva", "12345678901", "joao@example.com", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        verify(customerRepository).findById(1L);
        verify(customerRepository).delete(customer);
    }

    @Test
    void throwsWhenDeleteCustomerDoesNotExist() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class, () -> customerService.delete(1L));

        assertTrue(exception.getMessage().contains("1"));
        verify(customerRepository).findById(1L);
        verify(customerRepository, never()).delete(any(Customer.class));
    }

    @Test
    void returnsScoreWhenCustomerExists() {
        Customer customer = new Customer(
                "Joao Silva", "12345678901", "joao@example.com", CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        ScoreResponse scoreResponse = new ScoreResponse("12345678901", 750, "LOW_RISK");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(scoreClient.getScore(customer.getCpf())).thenReturn(scoreResponse);

        ScoreResponse response = customerService.getScore(1L);

        assertEquals(scoreResponse.getCpf(), response.getCpf());
        assertEquals(scoreResponse.getScore(), response.getScore());
        assertEquals(scoreResponse.getClassification(), response.getClassification());
        verify(customerRepository).findById(1L);
        verify(scoreClient).getScore(customer.getCpf());
    }

    @Test
    void throwsWhenGetScoreCustomerDoesNotExist() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class, () -> customerService.getScore(1L));

        assertTrue(exception.getMessage().contains("1"));
        verify(customerRepository).findById(1L);
        verify(scoreClient, never()).getScore(any(String.class));
    }
}
