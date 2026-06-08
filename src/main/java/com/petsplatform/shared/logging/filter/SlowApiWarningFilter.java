package com.petsplatform.shared.logging.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Detects and logs slow API responses as WARN messages.
 *
 * <p>Any request taking longer than {@code logging.slow-api.threshold-ms} (default: 1000ms)
 * will be logged with WARN level, including trace ID and duration.
 *
 * <p>Designed to run AFTER {@code RequestLoggingFilter} so MDC trace ID is available.
 *
 * <p>Example output:
 * <pre>
 * 2026-06-08 09:47:05 [http-nio-8080-exec-3] WARN  c.p.s.l.f.SlowApiWarningFilter -
 *   [SLOW_API] traceId="a1b2c3d4" GET /api/v1/reports/export duration=3521ms threshold=1000ms
 * </pre>
 *
 * <p>Configuration in application.yml:
 * <pre>
 * logging:
 *   slow-api:
 *     enabled: true
 *     threshold-ms: 1000
 *     paths-to-ignore: /actuator/health, /health
 * </pre>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class SlowApiWarningFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SlowApiWarningFilter.class);
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Value("${logging.slow-api.threshold-ms:1000}")
    private long thresholdMs;

    @Value("${logging.slow-api.enabled:true}")
    private boolean enabled;

    @Value("${logging.slow-api.paths-to-ignore:/actuator/health,/health}")
    private String pathsToIgnoreConfig;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!enabled ||
            !(request instanceof HttpServletRequest httpReq) ||
            !(response instanceof HttpServletResponse httpRes)) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpReq.getRequestURI();
        if (shouldIgnore(path)) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        int status = 0;

        try {
            chain.doFilter(request, response);
        } finally {
            status = httpRes.getStatus();
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > thresholdMs) {
            String traceId = org.slf4j.MDC.get(TRACE_ID_MDC_KEY);
            String method = httpReq.getMethod();

            log.warn("[SLOW_API] traceId=\"{}\" {} {} status={} duration={}ms threshold={}ms",
                traceId, method, path, status, duration, thresholdMs);
        }
    }

    private boolean shouldIgnore(String path) {
        String[] paths = pathsToIgnoreConfig.split(",");
        for (String p : paths) {
            if (path.contains(p.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
