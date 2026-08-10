package com.gamifiedjava.service;

import com.gamifiedjava.model.AiChat;
import com.gamifiedjava.model.AiConversation;
import com.gamifiedjava.repository.AiConversationRepository;
import com.gamifiedjava.repository.AiChatRepository;
import com.gamifiedjava.repository.ModuleRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OllamaServiceTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void validatesKeysWithAuthenticatedPsEndpoint() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = server("/api/ps", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{\"models\":[]}");
        });

        OllamaService service = service();

        assertTrue(service.validateApiKey("valid-key"));
        assertEquals("/api/ps", path.get());
        assertEquals("Bearer valid-key", authorization.get());
    }

    @Test
    void rejectsUnauthorizedKeys() throws Exception {
        server = server("/api/ps", exchange -> respond(exchange, 401, "{\"error\":\"unauthorized\"}"));

        assertFalse(service().validateApiKey("invalid-key"));
    }

    @Test
    void callsDirectCloudApiWithDirectModelName() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server("/api/generate", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"response\":\"Use Integer when you need an object.\"}");
        });
        UserAiSettingsService settings = mock(UserAiSettingsService.class);
        when(settings.apiKey("user-1")).thenReturn(Optional.of("valid-key"));
        OllamaService service = service(settings);

        String response = service.ask("int vs Integer", null, "ask", "user-1");

        assertEquals("Use Integer when you need an object.", response);
        assertTrue(requestBody.get().contains("\"model\":\"gemma4:31b\""));
    }

    @Test
    void sendsThreadHistoryAndPersistsMessagesInTheOwnedChat() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server("/api/generate", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"response\":\"Autoboxing converts it automatically.\"}");
        });
        AiChatRepository chats = mock(AiChatRepository.class);
        AiConversationRepository messages = mock(AiConversationRepository.class);
        UserAiSettingsService settings = mock(UserAiSettingsService.class);
        when(settings.apiKey("user-1")).thenReturn(Optional.of("valid-key"));

        AiChat chat = new AiChat();
        chat.setId(7);
        chat.setAuthUserId("user-1");
        chat.setTitle("New chat");
        AiConversation previous = new AiConversation();
        previous.setRole("user");
        previous.setMessage("What is boxing?");
        when(chats.findById(7)).thenReturn(Optional.of(chat));
        when(messages.findByChatIdOrderByCreatedAtAsc(7)).thenReturn(List.of(previous));

        OllamaService service = service(chats, messages, settings);
        service.ask("And autoboxing?", null, "ask", "user-1", 7);

        assertTrue(requestBody.get().contains("What is boxing?"));
        assertTrue(requestBody.get().contains("And autoboxing?"));
        assertEquals("And autoboxing?", chat.getTitle());
        verify(chats).save(chat);
        var saved = org.mockito.ArgumentCaptor.forClass(AiConversation.class);
        verify(messages, times(2)).save(saved.capture());
        assertTrue(saved.getAllValues().stream().allMatch(message -> Integer.valueOf(7).equals(message.getChatId())));
        assertTrue(saved.getAllValues().stream().allMatch(message -> "user-1".equals(message.getAuthUserId())));
    }

    @Test
    void refusesToOpenAnotherUsersChat() {
        AiChatRepository chats = mock(AiChatRepository.class);
        AiChat chat = new AiChat();
        chat.setId(9);
        chat.setAuthUserId("someone-else");
        when(chats.findById(9)).thenReturn(Optional.of(chat));
        OllamaService service = service(chats, mock(AiConversationRepository.class), mock(UserAiSettingsService.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.ask("Hello", null, "ask", "user-1", 9));
    }

    @Test
    void explainsProviderErrorsAccurately() {
        assertTrue(OllamaService.responseError(401).contains("API key"));
        assertTrue(OllamaService.responseError(404).contains("model"));
        assertTrue(OllamaService.responseError(429).contains("usage limit"));
        assertTrue(OllamaService.responseError(502).contains("temporarily unavailable"));
    }

    private OllamaService service() {
        return service(mock(UserAiSettingsService.class));
    }

    private OllamaService service(UserAiSettingsService settings) {
        return service(mock(AiChatRepository.class), mock(AiConversationRepository.class), settings);
    }

    private OllamaService service(AiChatRepository chats,
                                  AiConversationRepository messages,
                                  UserAiSettingsService settings) {
        OllamaService service = new OllamaService(
                chats,
                messages,
                mock(ModuleRepository.class),
                mock(GamificationService.class),
                settings
        );
        String testBaseUrl = server == null ? "http://127.0.0.1:1" : "http://127.0.0.1:" + server.getAddress().getPort();
        ReflectionTestUtils.setField(service, "baseUrl", testBaseUrl);
        ReflectionTestUtils.setField(service, "model", "gemma4:31b");
        ReflectionTestUtils.setField(service, "protocol", "ollama");
        return service;
    }

    private HttpServer server(String path, Handler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext(path, exchange -> handler.handle(exchange));
        httpServer.start();
        return httpServer;
    }

    private static void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
