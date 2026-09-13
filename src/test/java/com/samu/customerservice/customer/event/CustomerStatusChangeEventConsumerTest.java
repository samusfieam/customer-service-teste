package com.samu.customerservice.customer.event;

import com.samu.customerservice.customer.Customer;
import com.samu.customerservice.customer.CustomerRepository;
import com.samu.customerservice.customer.CustomerStatus;
import com.samu.customerservice.exception.CustomerNotFoundException;
import com.samu.customerservice.messaging.ProcessedEventRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerStatusChangeEventConsumerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private CustomerStatusChangeEventConsumer consumer;

    @Test
    void updatesCustomerStatusAndRegistersProcessedEvent() {
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "event-1",
                "CUSTOMER_STATUS_CHANGE",
                1L,
                CustomerStatus.INACTIVE);
        Customer customer = new Customer(
                "Maria Silva",
                "12345678901",
                "maria@example.com",
                CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(processedEventRepository.reserveEventIfAbsent(event.getEventId(), event.getEventType())).thenReturn(1);
        when(customerRepository.findById(event.getCustomerId())).thenReturn(Optional.of(customer));

        consumer.consume(event);

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        verify(processedEventRepository).reserveEventIfAbsent(event.getEventId(), event.getEventType());
        verify(customerRepository).save(customer);
    }

    @Test
    void doesNotProcessDuplicatedMessage() {
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "event-1",
                "CUSTOMER_STATUS_CHANGE",
                1L,
                CustomerStatus.INACTIVE);
        when(processedEventRepository.reserveEventIfAbsent(event.getEventId(), event.getEventType())).thenReturn(0);

        consumer.consume(event);

        verify(customerRepository, never()).findById(any(Long.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "event-1",
                "CUSTOMER_STATUS_CHANGE",
                1L,
                CustomerStatus.INACTIVE);
        when(processedEventRepository.reserveEventIfAbsent(event.getEventId(), event.getEventType())).thenReturn(1);
        when(customerRepository.findById(event.getCustomerId())).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> consumer.consume(event));

        verify(processedEventRepository).reserveEventIfAbsent(event.getEventId(), event.getEventType());
        verify(customerRepository).findById(event.getCustomerId());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void duplicatedMessageDoesNotApplyBusinessEffectTwice() {
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "event-1",
                "CUSTOMER_STATUS_CHANGE",
                1L,
                CustomerStatus.INACTIVE);
        Customer customer = new Customer(
                "Maria Silva",
                "12345678901",
                "maria@example.com",
                CustomerStatus.ACTIVE);
        ReflectionTestUtils.setField(customer, "id", 1L);
        when(processedEventRepository.reserveEventIfAbsent(event.getEventId(), event.getEventType()))
                .thenReturn(1)
                .thenReturn(0);
        when(customerRepository.findById(event.getCustomerId())).thenReturn(Optional.of(customer));

        consumer.consume(event);
        consumer.consume(event);

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        verify(processedEventRepository, times(2)).reserveEventIfAbsent(event.getEventId(), event.getEventType());
        verify(customerRepository, times(1)).findById(event.getCustomerId());
        verify(customerRepository, times(1)).save(customer);
    }
}
