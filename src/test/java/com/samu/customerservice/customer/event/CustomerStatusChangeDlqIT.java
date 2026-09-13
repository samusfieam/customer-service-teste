package com.samu.customerservice.customer.event;

import com.samu.customerservice.customer.CustomerRepository;
import com.samu.customerservice.customer.CustomerStatus;
import com.samu.customerservice.messaging.ProcessedEventRepository;
import com.samu.customerservice.messaging.RabbitMqConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(classes = CustomerStatusChangeDlqIT.TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerStatusChangeDlqIT {

    @Container
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3-management");

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        Mockito.reset(customerRepository, processedEventRepository);
        rabbitAdmin.initialize();
        rabbitAdmin.purgeQueue(RabbitMqConfig.CUSTOMER_STATUS_CHANGE_QUEUE, true);
        rabbitAdmin.purgeQueue(RabbitMqConfig.CUSTOMER_STATUS_CHANGE_DLQ, true);
    }

    @Test
    void sendsFailedStatusChangeMessageToDlqWithoutInfiniteRequeue() {
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "event-dlq-1",
                "CUSTOMER_STATUS_CHANGE",
                404L,
                CustomerStatus.INACTIVE);
        when(processedEventRepository.reserveEventIfAbsent(event.getEventId(), event.getEventType())).thenReturn(1);
        when(customerRepository.findById(event.getCustomerId())).thenReturn(Optional.empty());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CUSTOMER_EXCHANGE,
                RabbitMqConfig.CUSTOMER_STATUS_CHANGE_ROUTING_KEY,
                event);

        Message dlqMessage = receiveFromDlq();

        assertNotNull(dlqMessage);
        verify(customerRepository).findById(event.getCustomerId());
    }

    private Message receiveFromDlq() {
        long timeoutAt = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < timeoutAt) {
            Message message = rabbitTemplate.receive(RabbitMqConfig.CUSTOMER_STATUS_CHANGE_DLQ, 500);
            if (message != null) {
                return message;
            }
        }

        fail("Mensagem rejeitada nao chegou na DLQ dentro do tempo esperado.");
        return null;
    }

    @SpringBootConfiguration
    @EnableRabbit
    @Import({
            RabbitMqConfig.class,
            CustomerStatusChangeEventConsumer.class,
            CustomerStatusChangeDlqIT.RabbitMqTestConfig.class
    })
    static class TestApplication {
    }

    @TestConfiguration
    static class RabbitMqTestConfig {

        @Bean
        CachingConnectionFactory connectionFactory() {
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
                    rabbitMq.getHost(),
                    rabbitMq.getAmqpPort());
            connectionFactory.setUsername(rabbitMq.getAdminUsername());
            connectionFactory.setPassword(rabbitMq.getAdminPassword());
            return connectionFactory;
        }

        @Bean
        RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
            RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
            rabbitTemplate.setMessageConverter(messageConverter);
            return rabbitTemplate;
        }

        @Bean
        RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
            return new RabbitAdmin(connectionFactory);
        }

        @Bean
        CustomerRepository customerRepository() {
            return Mockito.mock(CustomerRepository.class);
        }

        @Bean
        ProcessedEventRepository processedEventRepository() {
            return Mockito.mock(ProcessedEventRepository.class);
        }
    }
}
