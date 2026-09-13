package resolvenow.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;

@Component
public class JwtAuthFilter implements Filter {

    @Value("${jwt.secret}")
    private String secret;

    private static final String[] PUBLIC_PATHS = {"/api/auth/register", "/api/auth/login"};

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

            HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper(request);
            wrappedRequest.addHeader("X-User-Id", claims.getSubject());
            wrappedRequest.addHeader("X-User-Role", claims.get("role", String.class));

            chain.doFilter(wrappedRequest, res);
            return;

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

//        chain.doFilter(req, res);
    }
}