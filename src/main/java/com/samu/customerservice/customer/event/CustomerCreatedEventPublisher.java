package com.samu.customerservice.customer.event;

import com.samu.customerservice.messaging.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerCreatedEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public CustomerCreatedEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(CustomerCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CUSTOMER_EXCHANGE,
                RabbitMqConfig.CUSTOMER_CREATED_ROUTING_KEY,
                event);
    }
}
