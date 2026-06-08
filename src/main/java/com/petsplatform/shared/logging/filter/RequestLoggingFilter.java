package com.petsplatform.shared.logging.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logs every HTTP request and response with:
 * <ul>
 *   <li>UUID trace ID (generated or from {@code X-Trace-Id} header)</li>
 *   <li>HTTP method, URL, status code, response time</li>
 *   <li>Masked sensitive fields (password, token, authorization...)</li>
 *   <li>Request body (masked) for POST/PUT/PATCH</li>
 *   <li>Response body (masked) for error responses</li>
 * </ul>
 *
 * <p>Filter order is {@link Ordered#HIGHEST_PRECEDENCE} + 1 to run
 * BEFORE {@code CharacterEncodingFilter} but AFTER security filters.
 *
 * <p>Output format compatible with ELK Stack (Elasticsearch/Logstash/Kibana):
 * <pre>
 * 2026-06-08 09:47:00.123 [http-nio-8080-exec-1] INFO  c.p.s.l.f.RequestLoggingFilter -
 *   traceId="a1b2c3d4" method="POST" uri="/api/v1/users" status=201 duration=45ms
 * </pre>
 *
 * @see com.petsplatform.shared.logging.MdcUtils
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
        "password", "pwd", "passwd",
        "authorization", "x-auth-token", "x-api-key",
        "bearer", "token", "refresh-token", "access-token",
        "secret", "private-key", "credential",
        "x-csrf-token", "csrf-token"
    );

    private static final Set<String> SENSITIVE_BODY_KEYS = Set.of(
        "password", "pwd", "passwd", "oldPassword", "newPassword", "confirmPassword",
        "currentPassword",
        "token", "accessToken", "refreshToken", "idToken",
        "secret", "apiKey", "privateKey", "credential",
        "cardNumber", "cvv", "cvc", "pan",
        "ssn", "socialSecurityNumber"
    );

    private static final Set<String> LOGGED_CONTENT_TYPES = Set.of(
        "application/json", "application/x-www-form-urlencoded"
    );

    private static final int MAX_BODY_LOG_LENGTH = 2000;

    private static final ConcurrentHashMap<String, byte[]> requestBodyCache = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpReq) ||
            !(response instanceof HttpServletResponse httpRes)) {
            chain.doFilter(request, response);
            return;
        }

        String traceId = resolveTraceId(httpReq);
        long startTime = System.currentTimeMillis();

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpReq);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(httpRes);

        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            int status = cachedResponse.getStatus();
            long duration = System.currentTimeMillis() - startTime;

            logRequest(cachedRequest, status, duration, traceId, requestId);

            if (status >= 400) {
                logResponseBody(cachedResponse, traceId);
            }

            cachedResponse.copyBodyToResponse();
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private void logRequest(CachedBodyHttpServletRequest request, int status,
                            long duration, String traceId, String requestId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            uri = uri + "?" + maskQueryParams(query);
        }

        String maskedBody = extractMaskedBody(request);

        if (log.isInfoEnabled()) {
            log.info("traceId=\"{}\" requestId=\"{}\" method=\"{}\" uri=\"{}\" status={} duration={}ms body={}",
                traceId, requestId, method, uri, status, duration,
                maskedBody.isEmpty() ? "" : maskedBody);
        } else if (log.isWarnEnabled() && status >= 400) {
            log.warn("traceId=\"{}\" requestId=\"{}\" method=\"{}\" uri=\"{}\" status={} duration={}ms",
                traceId, requestId, method, uri, status, duration);
        }
    }

    private String extractMaskedBody(CachedBodyHttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null || !isLoggedContentType(contentType)) {
            return "";
        }

        if (!isBodyLoggingEnabled(request.getMethod())) {
            return "[BODY_LOGGING_DISABLED]";
        }

        try {
            byte[] body = request.getCachedBody();
            if (body == null || body.length == 0) {
                return "";
            }
            String raw = new String(body, StandardCharsets.UTF_8);
            return maskSensitiveBody(raw);
        } catch (Exception e) {
            return "[BODY_PARSE_ERROR]";
        }
    }

    private void logResponseBody(ContentCachingResponseWrapper response, String traceId) {
        byte[] body = response.getContentAsByteArray();
        if (body.length == 0) {
            return;
        }

        String contentType = response.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            return;
        }

        try {
            String raw = new String(body, StandardCharsets.UTF_8);
            String masked = maskSensitiveBody(raw);
            if (masked.length() > MAX_BODY_LOG_LENGTH) {
                masked = masked.substring(0, MAX_BODY_LOG_LENGTH) + "...[TRUNCATED]";
            }
            log.warn("traceId=\"{}\" responseBody={}", traceId, masked);
        } catch (Exception e) {
            log.debug("Could not log response body", e);
        }
    }

    private boolean isLoggedContentType(String contentType) {
        return LOGGED_CONTENT_TYPES.stream().anyMatch(contentType::contains);
    }

    private boolean isBodyLoggingEnabled(String method) {
        return switch (method) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    public static String maskSensitiveBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }

        String masked = body;
        for (String key : SENSITIVE_BODY_KEYS) {
            masked = maskJsonField(masked, key);
        }
        return masked;
    }

    private static String maskJsonField(String json, String fieldName) {
        String[] patterns = {
            "\"" + fieldName + "\"\\s*:\\s*\"[^\"]*\"",
            "\"" + fieldName + "\"\\s*:\\s*\\[[^\\]]*\\]"
        };
        for (String pattern : patterns) {
            json = json.replaceAll("(?i)" + pattern,
                "\"" + fieldName + "\":\"[MASKED]\"");
        }
        return json;
    }

    private String maskQueryParams(String query) {
        StringBuilder sb = new StringBuilder();
        String[] params = query.split("&");
        for (int i = 0; i < params.length; i++) {
            String[] kv = params[i].split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            if (isSensitiveHeader(key)) {
                value = "***MASKED***";
            }
            sb.append(key).append("=").append(value);
            if (i < params.length - 1) sb.append("&");
        }
        return sb.toString();
    }

    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return SENSITIVE_HEADERS.stream().anyMatch(lower::contains);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {
        requestBodyCache.clear();
    }
}
