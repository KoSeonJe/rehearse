package com.rehearse.api.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_PREFIX = "/api/internal/";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String UNAUTHORIZED_RESPONSE =
            "{\"success\":false,\"code\":\"AUTH_001\",\"message\":\"유효하지 않은 내부 API 키입니다.\"}";

    @Value("${internal.api-key:}")
    private String internalApiKey;

    @PostConstruct
    void warnIfKeyNotConfigured() {
        if (internalApiKey.isEmpty()) {
            log.warn("internal.api-key가 설정되지 않았습니다. 내부 API가 인증 없이 노출됩니다.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith(INTERNAL_API_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId != null) {
            MDC.put(MDC_CORRELATION_ID, correlationId);
        }

        try {
            if (internalApiKey.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            String providedKey = request.getHeader(API_KEY_HEADER);
            if (providedKey == null || !providedKey.equals(internalApiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(UNAUTHORIZED_RESPONSE);
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
        }
    }
}
