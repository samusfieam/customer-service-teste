package com.samu.customerservice.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String CUSTOMER_EXCHANGE = "customer.exchange";
    public static final String CUSTOMER_CREATED_QUEUE = "customer.created.queue";
    public static final String CUSTOMER_STATUS_CHANGE_QUEUE = "customer.status.change.queue";
    public static final String CUSTOMER_CREATED_ROUTING_KEY = "customer.created";
    public static final String CUSTOMER_STATUS_CHANGE_ROUTING_KEY = "customer.status.change";

    @Bean
    public DirectExchange customerExchange() {
        return new DirectExchange(CUSTOMER_EXCHANGE);
    }

    @Bean
    public Queue customerCreatedQueue() {
        return QueueBuilder.durable(CUSTOMER_CREATED_QUEUE).build();
    }

    @Bean
    public Queue customerStatusChangeQueue() {
        return QueueBuilder.durable(CUSTOMER_STATUS_CHANGE_QUEUE).build();
    }

    @Bean
    public Binding customerCreatedBinding() {
        return BindingBuilder
                .bind(customerCreatedQueue())
                .to(customerExchange())
                .with(CUSTOMER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding customerStatusChangeBinding() {
        return BindingBuilder
                .bind(customerStatusChangeQueue())
                .to(customerExchange())
                .with(CUSTOMER_STATUS_CHANGE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
