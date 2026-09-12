package com.samu.customerservice.score;

import com.samu.customerservice.exception.ScoreServiceTimeoutException;
import com.samu.customerservice.exception.ScoreServiceUnavailableException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ScoreClient {

    private final RestClient restClient;

    public ScoreClient(@Value("${score.service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public ScoreResponse getScore(String cpf) {
        try {
            return restClient.get()
                    .uri("/scores/{cpf}", cpf)
                    .retrieve()
                    .body(ScoreResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is5xxServerError()) {
                throw new ScoreServiceUnavailableException(cpf);
            }

            throw exception;
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new ScoreServiceTimeoutException(cpf);
            }

            throw exception;
        }
    }

    private boolean isTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
