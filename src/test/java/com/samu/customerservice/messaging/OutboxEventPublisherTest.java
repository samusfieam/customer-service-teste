package com.samu.customerservice.messaging;

import com.samu.customerservice.customer.event.CustomerCreatedEvent;
import com.samu.customerservice.customer.event.CustomerCreatedEventPublisher;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CustomerCreatedEventPublisher customerCreatedEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void publishesPendingCustomerCreatedEventAndMarksAsPublished() throws Exception {
        String payload = "{\"eventType\":\"CUSTOMER_CREATED\"}";
        OutboxEvent outboxEvent = new OutboxEvent(
                "event-1",
                "CUSTOMER_CREATED",
                1L,
                payload,
                Instant.now(),
                null);
        CustomerCreatedEvent customerCreatedEvent = new CustomerCreatedEvent(
                "event-1",
                "CUSTOMER_CREATED",
                1L,
                "2026-08-11T15:30:00Z");
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));
        when(objectMapper.readValue(payload, CustomerCreatedEvent.class)).thenReturn(customerCreatedEvent);

        outboxEventPublisher.publishPendingEvents();

        ArgumentCaptor<CustomerCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CustomerCreatedEvent.class);
        verify(customerCreatedEventPublisher).publish(eventCaptor.capture());
        assertEquals(customerCreatedEvent, eventCaptor.getValue());

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertNotNull(outboxCaptor.getValue().getPublishedAt());
    }

    @Test
    void doesNotMarkAsPublishedWhenRabbitMqPublishFails() throws Exception {
        String payload = "{\"eventType\":\"CUSTOMER_CREATED\"}";
        OutboxEvent outboxEvent = new OutboxEvent(
                "event-1",
                "CUSTOMER_CREATED",
                1L,
                payload,
                Instant.now(),
                null);
        CustomerCreatedEvent customerCreatedEvent = new CustomerCreatedEvent(
                "event-1",
                "CUSTOMER_CREATED",
                1L,
                "2026-08-11T15:30:00Z");
        when(outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(outboxEvent));
        when(objectMapper.readValue(payload, CustomerCreatedEvent.class)).thenReturn(customerCreatedEvent);
        org.mockito.Mockito.doThrow(new AmqpException("RabbitMQ unavailable"))
                .when(customerCreatedEventPublisher)
                .publish(customerCreatedEvent);

        assertThrows(AmqpException.class, () -> outboxEventPublisher.publishPendingEvents());

        assertNull(outboxEvent.getPublishedAt());
        verify(outboxEventRepository, never()).save(outboxEvent);
    }
}
