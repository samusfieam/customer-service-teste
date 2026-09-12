package com.samu.customerservice.customer.event;

import com.samu.customerservice.customer.Customer;
import com.samu.customerservice.customer.CustomerRepository;
import com.samu.customerservice.customer.CustomerStatus;
import com.samu.customerservice.messaging.ProcessedEvent;
import com.samu.customerservice.messaging.ProcessedEventRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(customerRepository.findById(event.getCustomerId())).thenReturn(Optional.of(customer));

        consumer.consume(event);

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        verify(customerRepository).save(customer);

        ArgumentCaptor<ProcessedEvent> processedEventCaptor = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processedEventCaptor.capture());
        ProcessedEvent processedEvent = processedEventCaptor.getValue();
        assertEquals(event.getEventId(), processedEvent.getEventId());
        assertEquals(event.getEventType(), processedEvent.getEventType());
        assertNotNull(processedEvent.getProcessedAt());
    }

    @Test
    void doesNotProcessDuplicatedMessage() {
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "event-1",
                "CUSTOMER_STATUS_CHANGE",
                1L,
                CustomerStatus.INACTIVE);
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        consumer.consume(event);

        verify(customerRepository, never()).findById(any(Long.class));
        verify(customerRepository, never()).save(any(Customer.class));
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
