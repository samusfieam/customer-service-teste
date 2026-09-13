package com.samu.customerservice.customer.event;

import com.samu.customerservice.customer.Customer;
import com.samu.customerservice.customer.CustomerRepository;
import com.samu.customerservice.exception.CustomerNotFoundException;
import com.samu.customerservice.messaging.ProcessedEventRepository;
import com.samu.customerservice.messaging.RabbitMqConfig;
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
        int reservedEvents = processedEventRepository.reserveEventIfAbsent(
                event.getEventId(),
                event.getEventType());

        if (reservedEvents == 0) {
            return;
        }

        Customer customer = customerRepository.findById(event.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(event.getCustomerId()));

        customer.setStatus(event.getStatus());
        customerRepository.save(customer);
    }
}
