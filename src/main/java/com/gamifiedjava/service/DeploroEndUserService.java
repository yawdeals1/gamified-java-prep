package com.gamifiedjava.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Service
public class DeploroEndUserService {
    private final RestClient client;
    private final String projectApiUrl;

    public DeploroEndUserService(@Value("${deploro.api.base-url:}") String studioUrl,
                                 @Value("${deploro.api.token:}") String token) {
        String value = studioUrl == null ? "" : studioUrl.trim();
        int studio = value.lastIndexOf("/studio");
        this.projectApiUrl = studio > 0 ? value.substring(0, studio) : "";
        this.client = RestClient.builder().defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }

    public boolean delete(String authUserId) {
        if (projectApiUrl.isBlank() || authUserId == null || authUserId.isBlank()) return false;
        try {
            client.delete().uri(URI.create(projectApiUrl + "/auth/users/" + authUserId)).retrieve().toBodilessEntity();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
