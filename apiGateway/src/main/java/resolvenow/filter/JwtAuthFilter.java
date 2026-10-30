package resolvenow.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtAuthFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String secret;

    private static final String[] PUBLIC_PATHS = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/internal"   // Feign-internal endpoint, no user token available
    };

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                chain.doFilter(req, res);
                return;
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();
            String userId = claims.getSubject();
            if (isRateLimited(userId)) {
                response.setStatus(429); // 429 Too Many Requests
                return;
            }
            HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper(request);
            wrappedRequest.suppressHeader("X-Internal-Token");
            wrappedRequest.suppressHeader("X-User-Id");
            wrappedRequest.suppressHeader("X-User-Role");
            wrappedRequest.suppressHeader("X-User-Department");
            String role = claims.get("role", String.class);
            log.debug("Verified JWT user={} rolePresent={}", claims.getSubject(), role != null);
            wrappedRequest.addHeader("X-User-Id", claims.getSubject());
            wrappedRequest.addHeader("X-User-Role", role == null ? "" : role);
            log.debug("Forwarding identity headers userPresent={} rolePresent={}",
                    wrappedRequest.getHeader("X-User-Id") != null,
                    wrappedRequest.getHeader("X-User-Role") != null);

            chain.doFilter(wrappedRequest, res);
            return;

        } catch (Exception e) {
            log.warn("Rejected JWT for {}", path, e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
    }

    private static final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 60_000;

    private boolean isRateLimited(String userId) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = requestLog.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }
}