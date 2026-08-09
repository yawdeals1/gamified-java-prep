package com.gamifiedjava.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Minimal in-process rate limiter for expensive endpoints (/api/run, /ai/ask).
 * Fixed 60s window + a concurrency cap. Single-user app: in-memory state is
 * acceptable; move to a shared store if this app ever runs multi-node.
 */
@Component
public class RateLimiter {

    private static final int MAX_PER_WINDOW = 30;   // requests / 60s
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_CONCURRENT = 3;

    private final Semaphore concurrency = new Semaphore(MAX_CONCURRENT, true);
    private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger windowCount = new AtomicInteger();

    /** Returns true if the request may proceed; acquires a concurrent slot. */
    public boolean tryAcquire() {
        long now = System.currentTimeMillis();
        long start = windowStart.get();
        if (now - start > WINDOW_MS && windowStart.compareAndSet(start, now)) {
            windowCount.set(0);
        }
        if (windowCount.incrementAndGet() > MAX_PER_WINDOW) {
            windowCount.decrementAndGet();
            return false;
        }
        return concurrency.tryAcquire();
    }

    /** Releases the concurrent slot. Call in a finally block after tryAcquire. */
    public void release() {
        concurrency.release();
    }
}