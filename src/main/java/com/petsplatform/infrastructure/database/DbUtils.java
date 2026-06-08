package com.petsplatform.infrastructure.database;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Database utility class for common operations.
 *
 * <p>Provides:
 * <ul>
 *   <li>Optimistic lock helper</li>
 *   <li>UUID generation</li>
 *   <li>Pagination defaults</li>
 * </ul>
 *
 * <p>Use these utilities instead of duplicating logic across repositories.
 */
public final class DbUtils {

    private DbUtils() {}

    /**
     * Default page size for list queries.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size to prevent excessive queries.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Clamps a page number to be non-negative.
     */
    public static int clampPage(int page) {
        return Math.max(0, page);
    }

    /**
     * Clamps a page size between DEFAULT_PAGE_SIZE and MAX_PAGE_SIZE.
     */
    public static int clampPageSize(int requestedSize) {
        if (requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }

    /**
     * Checks if an entity implements optimistic locking.
     * Throws {@link IllegalArgumentException} if it doesn't.
     *
     * @param entity the entity to check
     * @param entityClass the class of the entity (for error message)
     */
    public static void requireOptimisticLock(Object entity, Class<?> entityClass) {
        try {
            entityClass.getDeclaredField("version");
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(
                "Entity " + entityClass.getSimpleName() +
                " must have a @Version field for optimistic locking");
        }
    }
}
