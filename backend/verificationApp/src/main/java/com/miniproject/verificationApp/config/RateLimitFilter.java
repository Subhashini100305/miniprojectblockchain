package com.miniproject.verificationApp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;

    private static final Map<String, Integer> LIMITS = Map.of(
            "POST /api/login", 10,
            "POST /api/register", 5,
            "POST /api/send-token", 3,
            "POST /api/verify-token", 10,
            "POST /api/verification/upload", 5,
            "POST /api/reviews/add", 5
    );

    private final ConcurrentHashMap<String, RequestWindow> requestWindows =
            new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String endpoint = request.getMethod() + " " + request.getRequestURI();
        Integer limit = LIMITS.get(endpoint);

        if (limit == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr() + ":" + endpoint;
        RequestWindow window = requestWindows.computeIfAbsent(
                key,
                ignored -> new RequestWindow(now)
        );

        if (!window.tryAcquire(now, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }

        if (requestWindows.size() > 1_000) {
            requestWindows.entrySet().removeIf(
                    entry -> now - entry.getValue().startedAt >= WINDOW_MILLIS
            );
        }

        filterChain.doFilter(request, response);
    }

    private static class RequestWindow {

        private long startedAt;
        private int count;

        private RequestWindow(long startedAt) {
            this.startedAt = startedAt;
        }

        private synchronized boolean tryAcquire(long now, int limit) {
            if (now - startedAt >= WINDOW_MILLIS) {
                startedAt = now;
                count = 0;
            }

            if (count >= limit) {
                return false;
            }

            count++;
            return true;
        }
    }
}
