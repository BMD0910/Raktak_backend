package com.raktakk.backend.service;

import com.raktakk.backend.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public void checkAuthLimit(String key) {
        Instant now = Instant.now();
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart.plusSeconds(WINDOW_SECONDS))) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (counter.counter.get() > MAX_ATTEMPTS) {
            throw new TooManyRequestsException("Trop de tentatives, réessayez dans une minute.");
        }
    }

    private record WindowCounter(Instant windowStart, AtomicInteger counter) {
    }
}
