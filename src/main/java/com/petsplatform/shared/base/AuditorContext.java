package com.petsplatform.shared.base;

import org.springframework.lang.Nullable;

/**
 * Thread-local holder for the current auditor (logged-in user).
 *
 * <p>Set this via {@link #setAuditor(String)} in a security filter, auth interceptor,
 * or service layer before persisting entities. It is consumed by
 * {@code @CreatedBy} / {@code @LastModifiedBy} fields on {@link BaseEntity}.
 *
 * <p>Example usage in a security filter:
 * <pre>
 * public class SecurityFilter {
 *     public void doFilter(...) {
 *         try {
 *             AuditorContext.setAuditor(SecurityContextHolder.getUser().getUsername());
 *             filterChain.doFilter(request, response);
 *         } finally {
 *             AuditorContext.clear();
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>Or for batch/async jobs where there is no user:
 * <pre>
 * AuditorContext.setAuditor("SYSTEM");
 * </pre>
 *
 * @see com.petsplatform.config.JpaAuditingConfig
 */
public final class AuditorContext {

    private static final ThreadLocal<String> AUDITOR = new ThreadLocal<>();

    private AuditorContext() {}

    /**
     * Sets the current auditor for this thread.
     * Overwrites any previous value.
     */
    public static void setAuditor(@Nullable String username) {
        AUDITOR.set(username);
    }

    /**
     * Returns the current auditor, or {@code null} if none was set.
     */
    @Nullable
    public static String getAuditor() {
        return AUDITOR.get();
    }

    /**
     * Removes the auditor for this thread.
     * Always call in a {@code finally} block to avoid ThreadLocal leaks.
     */
    public static void clear() {
        AUDITOR.remove();
    }
}
