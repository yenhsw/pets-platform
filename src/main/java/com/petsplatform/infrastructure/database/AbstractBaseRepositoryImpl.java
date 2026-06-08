package com.petsplatform.infrastructure.database;

import com.petsplatform.shared.base.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.Metamodel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

/**
 * Base repository implementation for all entities.
 *
 * <p>Extends {@link SimpleJpaRepository} to add:
 * <ul>
 *   <li>Soft-delete awareness in {@link #findById(Object)}</li>
 *   <li>Custom base class hook for every repository operation</li>
 * </ul>
 *
 * <p>Registered via {@code repositoryBaseClass} in {@link JpaDatabaseConfig}.
 *
 * @see BaseRepository
 * @see com.petsplatform.shared.base.BaseEntity
 */
@NoRepositoryBean
public class AbstractBaseRepositoryImpl<
        E extends BaseEntity,
        ID extends UUID
> extends SimpleJpaRepository<E, ID> implements BaseRepository<E, ID> {

    @PersistenceContext
    private EntityManager entityManager;

    private final Class<E> domainClass;

    public AbstractBaseRepositoryImpl(
            JpaMetamodelEntityInformation<E, ?> entityInformation,
            EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.domainClass = entityInformation.getJavaType();
        this.entityManager = entityManager;
    }

    /**
     * Overrides {@code findById} to exclude soft-deleted records.
     * Every {@code findById} call across all repositories automatically
     * filters out deleted entities.
     */
    @Override
    public Optional<E> findById(ID id) {
        return findByIdAndDeletedFalse(id);
    }

    @Override
    public Optional<E> findByIdAndDeletedFalse(ID id) {
        String jpql = """
            SELECT e FROM %s e
            WHERE e.id = :id AND e.deleted = false
            """.formatted(domainClass.getSimpleName());

        return Optional.ofNullable(entityManager
            .createQuery(jpql, domainClass)
            .setParameter("id", id)
            .getResultList()
            .stream()
            .findFirst()
            .orElse(null));
    }

    @Override
    public boolean existsByIdAndDeletedFalse(ID id) {
        String jpql = """
            SELECT COUNT(e) > 0 FROM %s e
            WHERE e.id = :id AND e.deleted = false
            """.formatted(domainClass.getSimpleName());

        return Boolean.TRUE.equals(entityManager
            .createQuery(jpql, Boolean.class)
            .setParameter("id", id)
            .getSingleResult());
    }

    /**
     * Paginates a query result, returning a {@link Page}.
     * Useful in custom queries inside repository implementations.
     */
    protected <T> Page<T> paginate(jakarta.persistence.Query query, Pageable pageable) {
        long total = ((Number) query.getResultList().size()).longValue();
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return new PageImpl<>(query.getResultList(), pageable, total);
    }
}
