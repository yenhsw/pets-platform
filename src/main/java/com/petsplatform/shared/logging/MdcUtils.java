package com.petsplatform.shared.logging;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Utility class for MDC (Mapped Diagnostic Context) operations.
 *
 * <p>MDC cho phép thêm context information vào mọi log statements
 * trong cùng một request thread — traceId, userId, requestId...
 *
 * <p>Usage:
 * <pre>
 * // Set trace ID
 * MdcUtils.putTraceId(traceId);
 *
 * // Set user context
 * MdcUtils.put("userId", userId);
 *
 * // Get trace ID
 * String traceId = MdcUtils.getTraceId();
 *
 * // Clear (ALWAYS in finally block)
 * MdcUtils.clear();
 * </pre>
 *
 * <p>Trace ID được include trong mọi log statement nhờ
 * {@code %X{traceId}} trong log pattern.
 *
 * @see com.petsplatform.shared.logging.filter.RequestLoggingFilter
 */
@Component
public class MdcUtils {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID  = "userId";
    public static final String REQUEST_ID = "requestId";

    private MdcUtils() {}

    /**
     * Adds trace ID to MDC. Called by {@code RequestLoggingFilter} on every request.
     */
    public static void putTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * Returns the current trace ID from MDC, or {@code null} if none was set.
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * Returns the value for a given MDC key, or {@code null} if not set.
     */
    public static String get(String key) {
        return key != null ? MDC.get(key) : null;
    }

    /**
     * Returns the current user ID from MDC, or {@code null} if none was set.
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /**
     * Adds user ID to MDC. Call this in your auth/security layer.
     */
    public static void putUserId(String userId) {
        MDC.put(USER_ID, userId);
    }

    /**
     * Adds a custom key-value pair to MDC.
     */
    public static void put(String key, String value) {
        if (key != null && value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * Removes a key from MDC.
     */
    public static void remove(String key) {
        MDC.remove(key);
    }

    /**
     * Clears ALL MDC entries. Always call in a {@code finally} block
     * after request processing to prevent ThreadLocal leaks.
     */
    public static void clear() {
        MDC.clear();
    }
}
