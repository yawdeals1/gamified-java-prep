package com.gamifiedjava.studio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for the Deploro Studio REST API (per-project auto-generated
 * table CRUD: /api/projects/:id/studio/*). Bearer-authenticated with a
 * project-scoped personal access token. Rows are plain JSON maps with
 * snake_case keys.
 */
@Component
public class StudioClient {

    private final RestClient restClient;
    private final String baseUrl;

    public StudioClient(@Value("${deploro.api.base-url:}") String baseUrl,
                        @Value("${deploro.api.token:}") String token) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        this.baseUrl = trimmed;
        if (trimmed.isBlank() || token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Deploro Studio API is not configured. Set DEPLORO_API_URL and DEPLORO_API_TOKEN.");
        }
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<Map<String, Object>> list(String table, Map<String, String> filters, int limit) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUri(URI.create(baseUrl + "/" + table))
                .queryParam("limit", limit >= 0 ? limit : 10000);
        if (filters != null) {
            filters.forEach((k, v) -> uri.queryParam("filter[" + k + "]", v));
        }
        Map json = callGet(uri.build().toUri());
        Object rows = json != null ? json.get("rows") : null;
        if (rows instanceof List<?> list) {
            return list.stream().map(r -> (Map<String, Object>) r).toList();
        }
        return List.of();
    }

    public Map<String, Object> byId(String table, Object id) {
        try {
            Map json = callGet(URI.create(baseUrl + "/" + table + "/" + id));
            return json != null && json.get("row") instanceof Map<?, ?> row ? (Map<String, Object>) row : null;
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public Map<String, Object> insert(String table, Map<String, Object> row) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restClient.post()
                .uri(URI.create(baseUrl + "/" + table))
                .body(row)
                .retrieve()
                .body(Map.class);
        return resp != null && resp.get("row") instanceof Map<?, ?> r ? (Map<String, Object>) r : null;
    }

    public Map<String, Object> update(String table, Object id, Map<String, Object> row) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restClient.patch()
                .uri(URI.create(baseUrl + "/" + table + "/" + id))
                .body(row)
                .retrieve()
                .body(Map.class);
        return resp != null && resp.get("row") instanceof Map<?, ?> r ? (Map<String, Object>) r : null;
    }

    public void delete(String table, Object id) {
        restClient.delete()
                .uri(URI.create(baseUrl + "/" + table + "/" + id))
                .retrieve()
                .toBodilessEntity();
    }

    public long count(String table) {
        Map json = callGet(URI.create(baseUrl + "/" + table + "?limit=1"));
        Object total = json != null ? json.get("total") : null;
        return total instanceof Number n ? n.longValue() : 0;
    }

    private Map callGet(URI uri) {
        @SuppressWarnings("unchecked")
        Map<String, Object> json = restClient.get()
                .uri(uri)
                .retrieve()
                .body(Map.class);
        return json;
    }

    public static Map<String, String> filter(String column, Object value) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put(column, String.valueOf(value));
        return f;
    }
}