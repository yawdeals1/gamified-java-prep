package com.gamifiedjava.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/** Per-identity fixed-window limiter with global and per-user concurrency caps. */
@Component
public class RateLimiter {
    private static final long WINDOW_MS = 60_000;
    private static final Semaphore GLOBAL_CONCURRENCY = new Semaphore(8, true);

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Map<String, Semaphore> concurrentByKey = new ConcurrentHashMap<>();

    public Lease tryAcquire(String category, String identity) {
        String safeCategory = category == null ? "general" : category;
        String safeIdentity = identity == null || identity.isBlank() ? "anonymous" : identity;
        String key = safeCategory + ":" + safeIdentity;
        int limit = switch (safeCategory) {
            case "auth" -> 10;
            case "challenge" -> 5;
            case "code" -> 12;
            case "ai" -> 20;
            default -> 30;
        };

        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || now - current.startedAt >= WINDOW_MS ? new Window(now) : current);
        synchronized (window) {
            if (window.count >= limit) return null;
            window.count++;
        }

        Semaphore perKey = concurrentByKey.computeIfAbsent(key, ignored -> new Semaphore(2, true));
        if (!GLOBAL_CONCURRENCY.tryAcquire()) return null;
        if (!perKey.tryAcquire()) {
            GLOBAL_CONCURRENCY.release();
            return null;
        }
        return new Lease(perKey);
    }

    private static final class Window {
        private final long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
    }

    public static final class Lease implements AutoCloseable {
        private final Semaphore perKey;
        private boolean closed;
        private Lease(Semaphore perKey) { this.perKey = perKey; }
        @Override public synchronized void close() {
            if (closed) return;
            closed = true;
            perKey.release();
            GLOBAL_CONCURRENCY.release();
        }
    }
}
