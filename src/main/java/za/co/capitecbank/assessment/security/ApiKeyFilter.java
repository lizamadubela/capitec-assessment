package za.co.capitecbank.assessment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    @Value("${api.key.name}")
    private String apiKeyName;

    @Value("${api.key.value}")
    private String apiKeyValue;

    //paths that don't require API key
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/public/",
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Skip API key check for public endpoints
        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestApiKey = request.getHeader(apiKeyName);

        if (requestApiKey == null || !requestApiKey.equals(apiKeyValue)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized: Invalid API Key\"}");
            return;
        }

        // Create an Authentication object and set it in the SecurityContext
        ApiKeyAuthentication authentication = new ApiKeyAuthentication(requestApiKey, true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isPublicPath(String requestPath) {
        return PUBLIC_PATHS.stream().anyMatch(requestPath::startsWith);
    }
}