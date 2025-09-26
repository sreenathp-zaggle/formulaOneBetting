package org.example.formulaone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.formulaone.exceptions.HttpClientException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class WebClientHttpClient implements IHttpClient {
    private final WebClient webClient;
    private final ObjectMapper mapper;

    public WebClientHttpClient(WebClient.Builder builder, ObjectMapper mapper) {
        this.webClient = builder.build();
        this.mapper = mapper;
    }

    @Override
    public JsonNode getJson(String url, Map<String, String> queryParams) {
        String body = get(url, queryParams);

        try {
            return mapper.readTree(body);
        } catch (Exception ex) {
            throw new HttpClientException("Failed to parse JSON", ex);
        }
    }

    @Override
    public String get(String url, Map<String, String> queryParams) {
        String uri = buildUri(url, queryParams);

        // Perform a GET request with WebClient
        return webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        clientResponse -> Mono.error(
                                new HttpClientException("Non-2xx from provider: " + clientResponse.statusCode())))
                .bodyToMono(String.class)
                .block();
    }

    private String buildUri(String url, Map<String, String> queryParams) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null) {
            queryParams.forEach(ub::queryParam);
        }
        return ub.build().encode().toUriString();
    }
}
