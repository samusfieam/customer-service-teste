package com.samu.customerservice.score;

import com.samu.customerservice.exception.ScoreServiceTimeoutException;
import com.samu.customerservice.exception.ScoreServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreClientTest {

    private HttpServer httpServer;
    private ScoreClient scoreClient;

    @BeforeEach
    void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/scores/12345678901", exchange -> {
            String responseBody = """
                    {
                      "cpf": "12345678901",
                      "score": 750,
                      "classification": "LOW_RISK"
                    }
                    """;
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        httpServer.createContext("/scores/99999999999", exchange -> {
            String responseBody = "{\"message\":\"Score service failed\"}";
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        httpServer.createContext("/scores/88888888888", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            String responseBody = """
                    {
                      "cpf": "88888888888",
                      "score": 500,
                      "classification": "MEDIUM_RISK"
                    }
                    """;
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        httpServer.start();

        String baseUrl = "http://localhost:" + httpServer.getAddress().getPort();
        scoreClient = new ScoreClient(baseUrl);
    }

    @AfterEach
    void tearDown() {
        httpServer.stop(0);
    }

    @Test
    void returnsScoreSuccessfully() {
        ScoreResponse response = scoreClient.getScore("12345678901");

        assertEquals("12345678901", response.getCpf());
        assertEquals(750, response.getScore());
        assertEquals("LOW_RISK", response.getClassification());
    }

    @Test
    void throwsUnavailableWhenScoreServiceReturnsServerError() {
        assertThrows(ScoreServiceUnavailableException.class, () -> scoreClient.getScore("99999999999"));
    }

    @Test
    void throwsTimeoutWhenScoreServiceTakesMoreThanTwoSeconds() {
        assertThrows(ScoreServiceTimeoutException.class, () -> scoreClient.getScore("88888888888"));
    }
}
