package com.samu.customerservice.customer.event;

import com.samu.customerservice.customer.Customer;
import com.samu.customerservice.customer.CustomerRepository;
import com.samu.customerservice.customer.CustomerStatus;
import com.samu.customerservice.messaging.ProcessedEvent;
import com.samu.customerservice.messaging.ProcessedEventRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CustomerStatusChangeEventConsumer.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CustomerStatusChangeIdempotencyIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private CustomerStatusChangeEventConsumer consumer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Test
    void concurrentMessagesWithSameEventIdApplyBusinessEffectOnlyOnce() throws Exception {
        Customer customer = customerRepository.saveAndFlush(new Customer(
                "Maria Silva",
                "12345678901",
                "maria@example.com",
                CustomerStatus.ACTIVE));
        CustomerStatusChangeEvent event = new CustomerStatusChangeEvent(
                "event-concurrent-1",
                "CUSTOMER_STATUS_CHANGE",
                customer.getId(),
                CustomerStatus.INACTIVE);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            Future<?> firstAttempt = executorService.submit(() -> consumeAfterStartSignal(event, startSignal));
            Future<?> secondAttempt = executorService.submit(() -> consumeAfterStartSignal(event, startSignal));

            startSignal.countDown();

            firstAttempt.get(10, TimeUnit.SECONDS);
            secondAttempt.get(10, TimeUnit.SECONDS);
        } finally {
            executorService.shutdownNow();
        }

        Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        List<ProcessedEvent> processedEvents = processedEventRepository.findAll();

        assertEquals(CustomerStatus.INACTIVE, updatedCustomer.getStatus());
        assertEquals(1, processedEvents.size());
        assertEquals("event-concurrent-1", processedEvents.get(0).getEventId());
        assertTrue(processedEvents.stream()
                .allMatch(processedEvent -> "CUSTOMER_STATUS_CHANGE".equals(processedEvent.getEventType())));
    }

    private void consumeAfterStartSignal(CustomerStatusChangeEvent event, CountDownLatch startSignal) {
        try {
            startSignal.await(5, TimeUnit.SECONDS);
            consumer.consume(event);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido ao iniciar processamento concorrente.", exception);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("com.samu.customerservice")
    @EnableJpaRepositories("com.samu.customerservice")
    static class TestApplication {
    }
}
