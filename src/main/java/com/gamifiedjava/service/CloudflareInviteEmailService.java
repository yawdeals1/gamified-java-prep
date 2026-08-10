package com.gamifiedjava.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CloudflareInviteEmailService {
    private final RestClient client = RestClient.builder().build();
    private final String accountId;
    private final String apiToken;
    private final String fromAddress;
    private final String fromName;

    public CloudflareInviteEmailService(
            @Value("${cloudflare.email.account-id:}") String accountId,
            @Value("${cloudflare.email.api-token:}") String apiToken,
            @Value("${cloudflare.email.from-address:}") String fromAddress,
            @Value("${cloudflare.email.from-name:JAVA_CORE}") String fromName) {
        this.accountId = clean(accountId);
        this.apiToken = clean(apiToken);
        this.fromAddress = clean(fromAddress);
        this.fromName = clean(fromName);
    }

    public boolean isConfigured() {
        return !accountId.isBlank() && !apiToken.isBlank() && !fromAddress.isBlank();
    }

    public void sendInvite(String email, String inviteUrl) {
        if (!isConfigured()) throw new IllegalStateException("Invitation email is not configured on this server.");
        String safeUrl = HtmlUtils.htmlEscape(inviteUrl);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to", email);
        body.put("subject", "You are invited to JAVA_CORE");
        body.put("text", "You have been invited to JAVA_CORE. Create your member account: " + inviteUrl + "\n\nThis link expires in 48 hours.");
        body.put("html", "<div style=\"font-family:Arial,sans-serif;color:#12201b\"><h2>You are invited to JAVA_CORE</h2>" +
                "<p>An administrator invited you to join as a member.</p><p><a href=\"" + safeUrl +
                "\" style=\"display:inline-block;background:#4edea3;color:#07110d;padding:12px 18px;border-radius:8px;text-decoration:none;font-weight:700\">Create member account</a></p>" +
                "<p style=\"color:#66736e;font-size:13px\">Your email is already filled in. Add your name and password to finish. This link expires in 48 hours.</p></div>");

        body.put("from", Map.of("address", fromAddress, "name", fromName));
        client.post()
                .uri(URI.create("https://api.cloudflare.com/client/v4/accounts/" + accountId + "/email/sending/send"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
