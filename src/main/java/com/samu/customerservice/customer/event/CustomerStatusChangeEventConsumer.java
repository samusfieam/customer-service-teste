package com.samu.customerservice.customer.event;

import com.samu.customerservice.customer.Customer;
import com.samu.customerservice.customer.CustomerRepository;
import com.samu.customerservice.exception.CustomerNotFoundException;
import com.samu.customerservice.messaging.ProcessedEvent;
import com.samu.customerservice.messaging.ProcessedEventRepository;
import com.samu.customerservice.messaging.RabbitMqConfig;
import java.time.Instant;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustomerStatusChangeEventConsumer {

    private final CustomerRepository customerRepository;
    private final ProcessedEventRepository processedEventRepository;

    public CustomerStatusChangeEventConsumer(
            CustomerRepository customerRepository,
            ProcessedEventRepository processedEventRepository) {
        this.customerRepository = customerRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @RabbitListener(queues = RabbitMqConfig.CUSTOMER_STATUS_CHANGE_QUEUE)
    @Transactional
    public void consume(CustomerStatusChangeEvent event) {
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            return;
        }

        Customer customer = customerRepository.findById(event.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(event.getCustomerId()));

        customer.setStatus(event.getStatus());
        customerRepository.save(customer);

        ProcessedEvent processedEvent = new ProcessedEvent(
                event.getEventId(),
                event.getEventType(),
                Instant.now());
        processedEventRepository.save(processedEvent);
    }
}
