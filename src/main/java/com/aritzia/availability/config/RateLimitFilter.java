package com.aritzia.availability.config;

import com.aritzia.availability.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client-IP token bucket rate limiter. Runs ahead of the controller so a
 * throttled caller never reaches business logic. This is application-level,
 * fine-grained rate limiting; edge-level protection (Cloud Armor) is a
 * separate, coarser layer in front of this service in production.
 */
@Component
public class RateLimitFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int CAPACITY = 20;
    private static final Duration REFILL_PERIOD = Duration.ofSeconds(1);

    private final Map<String, Bucket> bucketsByClientIp = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String clientIp = clientIp(request);
        Bucket bucket = bucketsByClientIp.computeIfAbsent(clientIp, ip -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for client={}", clientIp);
        response.setStatus(429);
        response.setHeader("Retry-After", "1");
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(429, "Too Many Requests", "Rate limit exceeded, retry shortly"));
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(CAPACITY, REFILL_PERIOD));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
