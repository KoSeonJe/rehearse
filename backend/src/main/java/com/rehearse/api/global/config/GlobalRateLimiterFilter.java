package com.rehearse.api.global.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "api.global-concurrency-limit", matchIfMissing = false)
public class GlobalRateLimiterFilter implements Filter {

    private final Semaphore semaphore;
    private final int limit;

    public GlobalRateLimiterFilter(@Value("${api.global-concurrency-limit:0}") int limit) {
        this.limit = limit;
        this.semaphore = new Semaphore(limit, true);
        if (limit > 0) {
            log.info("GlobalRateLimiterFilter 활성화: 동시 요청 제한 = {}", limit);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (limit <= 0) {
            chain.doFilter(request, response);
            return;
        }

        if (!semaphore.tryAcquire()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write(
                    "{\"status\":503,\"code\":\"RATE_LIMITED\",\"message\":\"서버 동시 요청 한도 초과\"}");
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            semaphore.release();
        }
    }
}
