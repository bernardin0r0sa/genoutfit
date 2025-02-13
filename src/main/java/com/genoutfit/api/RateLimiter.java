package com.genoutfit.api;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Rate limiter as a component
@Component
public class RateLimiter {
    private final Bucket bucket = Bucket4j.builder()
            .addLimit(Bandwidth.classic(9, Refill.intervally(9, Duration.ofSeconds(1))))
            .build();

    public void acquirePermission() throws InterruptedException {
        while (!bucket.tryConsume(1)) {
            Thread.sleep(100);
        }
    }
}