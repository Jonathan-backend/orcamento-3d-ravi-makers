package br.com.orcamento3d.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSecurityFilter extends OncePerRequestFilter {
    private static final long WINDOW_SECONDS = 60;
    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        addSecurityHeaders(request, response);
        if (isCrossSiteMutation(request)) {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Requisição de outra origem bloqueada.\"}");
            return;
        }
        int limit = requestLimit(request);
        if (limit > 0 && exceeded(request, limit)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", Long.toString(WINDOW_SECONDS));
            response.getWriter().write("{\"message\":\"Muitas tentativas. Aguarde um minuto e tente novamente.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isCrossSiteMutation(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/api/")) return false;
        if (!java.util.Set.of("POST", "PUT", "PATCH", "DELETE")
                .contains(request.getMethod().toUpperCase())) return false;
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        if ("cross-site".equalsIgnoreCase(fetchSite)) return true;
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            // Navegadores modernos enviam Origin ou Sec-Fetch-Site. Requisições
            // autenticadas por cookie sem ambos são rejeitadas; clientes de API
            // continuam podendo usar Authorization: Bearer.
            return (fetchSite == null || fetchSite.isBlank()) && hasAuthCookie(request)
                    && !hasBearerToken(request);
        }
        String expected = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                ? "" : ":" + request.getServerPort());
        return !origin.equalsIgnoreCase(expected);
    }

    private boolean hasAuthCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return false;
        return java.util.Arrays.stream(request.getCookies())
                .anyMatch(cookie -> "ORCA_AUTH".equals(cookie.getName()) && !cookie.getValue().isBlank());
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer ")
                && authorization.length() > 7;
    }

    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
                "form-action 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; " +
                "script-src 'self' 'unsafe-inline'; connect-src 'self';");
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if (request.getRequestURI().startsWith("/api/auth/") || request.getRequestURI().equals("/oauth/callback")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }
    }

    private int requestLimit(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return 0;
        return switch (request.getRequestURI()) {
            case "/api/auth/login" -> 10;
            case "/api/auth/register" -> 5;
            case "/api/budgets/analyze" -> 12;
            default -> 0;
        };
    }

    private boolean exceeded(HttpServletRequest request, int limit) {
        long now = Instant.now().getEpochSecond();
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        Window window = attempts.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= WINDOW_SECONDS) {
                return new Window(now);
            }
            current.count.incrementAndGet();
            return current;
        });
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_SECONDS);
        }
        return window.count.get() > limit;
    }

    private static final class Window {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger(1);
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
