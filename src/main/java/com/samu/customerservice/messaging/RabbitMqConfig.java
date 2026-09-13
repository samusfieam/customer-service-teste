package com.samu.customerservice.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String CUSTOMER_EXCHANGE = "customer.exchange";
    public static final String CUSTOMER_CREATED_QUEUE = "customer.created.queue";
    public static final String CUSTOMER_STATUS_CHANGE_QUEUE = "customer.status.change.queue";
    public static final String CUSTOMER_STATUS_CHANGE_DLQ = "customer.status.change.dlq";
    public static final String CUSTOMER_CREATED_ROUTING_KEY = "customer.created";
    public static final String CUSTOMER_STATUS_CHANGE_ROUTING_KEY = "customer.status.change";
    public static final String CUSTOMER_STATUS_CHANGE_DLQ_ROUTING_KEY = "customer.status.change.dlq";

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
        return QueueBuilder
                .durable(CUSTOMER_STATUS_CHANGE_QUEUE)
                .deadLetterExchange(CUSTOMER_EXCHANGE)
                .deadLetterRoutingKey(CUSTOMER_STATUS_CHANGE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue customerStatusChangeDlq() {
        return QueueBuilder.durable(CUSTOMER_STATUS_CHANGE_DLQ).build();
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
    public Binding customerStatusChangeDlqBinding() {
        return BindingBuilder
                .bind(customerStatusChangeDlq())
                .to(customerExchange())
                .with(CUSTOMER_STATUS_CHANGE_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
