package com.samu.customerservice.messaging;

import com.samu.customerservice.customer.event.CustomerCreatedEvent;
import com.samu.customerservice.customer.event.CustomerCreatedEventPublisher;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        RabbitMqConfig.class,
        CustomerCreatedEventPublisher.class,
        OutboxEventPublisher.class,
        OutboxEventPublisherIT.RabbitMqTestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxEventPublisherIT {

    private static final Network network = Network.newNetwork();

    private static ToxiproxyContainer.ContainerProxy rabbitMqProxy;

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static final RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3-management")
            .withNetwork(network)
            .withNetworkAliases("rabbitmq");

    @Container
    static final ToxiproxyContainer toxiproxy = new ToxiproxyContainer(
            DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0"))
            .withNetwork(network);

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private CachingConnectionFactory connectionFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @AfterEach
    void restoreRabbitMqConnection() throws Exception {
        rabbitMqProxy().setConnectionCut(false);
        connectionFactory.resetConnection();
    }

    @Test
    void keepsOutboxEventPendingWhenRabbitMqIsUnavailableAndPublishesAfterRabbitMqReturns() throws Exception {
        rabbitAdmin.initialize();
        rabbitAdmin.purgeQueue(RabbitMqConfig.CUSTOMER_CREATED_QUEUE, true);

        CustomerCreatedEvent customerCreatedEvent = new CustomerCreatedEvent(
                "event-1",
                "CUSTOMER_CREATED",
                123L,
                "2026-08-11T15:30:00Z");
        OutboxEvent outboxEvent = new OutboxEvent(
                customerCreatedEvent.getEventId(),
                customerCreatedEvent.getEventType(),
                customerCreatedEvent.getCustomerId(),
                objectMapper.writeValueAsString(customerCreatedEvent),
                Instant.now(),
                null);
        OutboxEvent savedOutboxEvent = outboxEventRepository.saveAndFlush(outboxEvent);

        rabbitMqProxy().setConnectionCut(true);
        connectionFactory.resetConnection();

        assertThrows(AmqpException.class, () -> outboxEventPublisher.publishPendingEvents());

        Optional<OutboxEvent> pendingEvent = outboxEventRepository.findById(savedOutboxEvent.getId());
        assertNotNull(pendingEvent.orElseThrow().getId());
        assertNull(pendingEvent.orElseThrow().getPublishedAt());

        rabbitMqProxy().setConnectionCut(false);
        connectionFactory.resetConnection();
        waitForRabbitMq();

        outboxEventPublisher.publishPendingEvents();

        Message publishedMessage = rabbitTemplate.receive(
                RabbitMqConfig.CUSTOMER_CREATED_QUEUE,
                5000);
        assertNotNull(publishedMessage);
        String publishedPayload = new String(publishedMessage.getBody(), StandardCharsets.UTF_8);
        CustomerCreatedEvent publishedEvent = objectMapper.readValue(
                publishedPayload,
                CustomerCreatedEvent.class);
        assertEquals(customerCreatedEvent.getEventId(), publishedEvent.getEventId());
        assertEquals(customerCreatedEvent.getEventType(), publishedEvent.getEventType());
        assertEquals(customerCreatedEvent.getCustomerId(), publishedEvent.getCustomerId());
        assertEquals(customerCreatedEvent.getCreatedAt(), publishedEvent.getCreatedAt());

        OutboxEvent publishedOutboxEvent = outboxEventRepository.findById(savedOutboxEvent.getId()).orElseThrow();
        assertNotNull(publishedOutboxEvent.getPublishedAt());
    }

    private void waitForRabbitMq() {
        long timeoutAt = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < timeoutAt) {
            try {
                connectionFactory.resetConnection();
                rabbitAdmin.initialize();
                return;
            } catch (AmqpException exception) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrompido ao aguardar RabbitMQ.", interruptedException);
                }
            }
        }

        fail("RabbitMQ nao ficou disponivel dentro do tempo esperado.");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("com.samu.customerservice")
    @EnableJpaRepositories("com.samu.customerservice")
    static class TestApplication {
    }

    @TestConfiguration
    static class RabbitMqTestConfig {

        @Bean
        CachingConnectionFactory connectionFactory() {
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
                    rabbitMqProxy().getContainerIpAddress(),
                    rabbitMqProxy().getProxyPort());
            connectionFactory.setUsername(rabbitMq.getAdminUsername());
            connectionFactory.setPassword(rabbitMq.getAdminPassword());
            connectionFactory.getRabbitConnectionFactory().setConnectionTimeout(2000);
            connectionFactory.getRabbitConnectionFactory().setRequestedHeartbeat(2);
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
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static ToxiproxyContainer.ContainerProxy rabbitMqProxy() {
        if (rabbitMqProxy == null) {
            rabbitMqProxy = toxiproxy.getProxy(rabbitMq, 5672);
        }

        return rabbitMqProxy;
    }
}
