package com.samu.customerservice.messaging;

import com.samu.customerservice.customer.event.CustomerCreatedEvent;
import com.samu.customerservice.customer.event.CustomerCreatedEventPublisher;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxEventPublisher {

    private static final String CUSTOMER_CREATED = "CUSTOMER_CREATED";

    private final OutboxEventRepository outboxEventRepository;
    private final CustomerCreatedEventPublisher customerCreatedEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            CustomerCreatedEventPublisher customerCreatedEventPublisher,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.customerCreatedEventPublisher = customerCreatedEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent outboxEvent : pendingEvents) {
            if (CUSTOMER_CREATED.equals(outboxEvent.getEventType())) {
                publishCustomerCreated(outboxEvent);
            }
        }
    }

    private void publishCustomerCreated(OutboxEvent outboxEvent) {
        try {
            CustomerCreatedEvent event = objectMapper.readValue(
                    outboxEvent.getPayload(), CustomerCreatedEvent.class);

            customerCreatedEventPublisher.publish(event);

            // At-least-once: if RabbitMQ receives the event and this save fails,
            // the event can be published again. Consumers must be idempotent.
            outboxEvent.setPublishedAt(Instant.now());
            outboxEventRepository.save(outboxEvent);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Nao foi possivel desserializar evento da outbox.", exception);
        }
    }
}
