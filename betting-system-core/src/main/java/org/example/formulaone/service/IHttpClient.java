package org.example.formulaone.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.formulaone.exceptions.HttpClientException;

import java.util.Map;

public interface IHttpClient {
    /**
     * Perform a GET request and parse the response body as JSON.
     *
     * @param url the base URL (without query params)
     * @param queryParams map of query params (may be null or empty)
     * @return response body parsed as JsonNode
     * @throws HttpClientException if network error or non-2xx status
     */
    JsonNode getJson(String url, Map<String, String> queryParams);

    /**
     * Perform a GET request and return raw response body as String.
     *
     * @param url base URL
     * @param queryParams map of query params
     * @return raw body
     */
    String get(String url, Map<String, String> queryParams);
}
