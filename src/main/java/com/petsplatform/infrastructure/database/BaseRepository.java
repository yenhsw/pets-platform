package com.petsplatform.infrastructure.database;

import com.petsplatform.shared.base.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface for all entities.
 *
 * <p>Every domain repository in the platform extends this interface,
 * which provides:
 * <ul>
 *   <li>Standard CRUD operations via {@link JpaRepository}</li>
 *   <li>Dynamic query via {@link JpaSpecificationExecutor}</li>
 *   <li>Custom base implementation in {@link AbstractBaseRepositoryImpl}</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * public interface UserRepository extends BaseRepository&lt;User, UUID&gt; {
 *     Optional&lt;User&gt; findByEmail(String email);
 * }
 * </pre>
 *
 * @see AbstractBaseRepositoryImpl
 * @see JpaSpecificationExecutor
 * @see com.petsplatform.shared.base.BaseEntity
 */
@NoRepositoryBean
public interface BaseRepository<
        E extends BaseEntity,
        ID extends UUID
> extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {

    /**
     * Finds an entity by ID, excluding soft-deleted records.
     *
     * <p>Use this instead of {@code findById(id)} to automatically
     * filter out soft-deleted records.
     */
    Optional<E> findByIdAndDeletedFalse(ID id);

    /**
     * Checks if an entity exists and is not soft-deleted.
     */
    boolean existsByIdAndDeletedFalse(ID id);
}
