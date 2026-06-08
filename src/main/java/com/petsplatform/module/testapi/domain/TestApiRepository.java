package com.petsplatform.module.testapi.domain;

import com.petsplatform.infrastructure.database.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestApiRepository extends BaseRepository<TestApi, UUID>,
        JpaSpecificationExecutor<TestApi> {

    /* ── Method name queries ──────────────────────────────── */

    Optional<TestApi> findByEmail(String email);

    boolean existsByEmail(String email);

    List<TestApi> findByNameContainingIgnoreCase(String name);

    List<TestApi> findByStatus(TestApi.TestApiStatus status);

    Page<TestApi> findByStatus(TestApi.TestApiStatus status, Pageable pageable);

    List<TestApi> findByGender(TestApi.Gender gender);

    Page<TestApi> findByGender(TestApi.Gender gender, Pageable pageable);

    List<TestApi> findByAgeBetween(Integer minAge, Integer maxAge);

    List<TestApi> findByBirthDateAfter(LocalDate date);

    List<TestApi> findByScoreGreaterThanEqual(Double minScore);

    /* ── @Query — JPQL ─────────────────────────────────── */

    @Query("""
        SELECT t FROM TestApi t
        WHERE t.name LIKE %:name%
          AND t.status = :status
          AND t.deleted = false
        ORDER BY t.createdAt DESC
        """)
    List<TestApi> searchByNameAndStatus(
        @Param("name") String name,
        @Param("status") TestApi.TestApiStatus status
    );

    @Query("""
        SELECT t FROM TestApi t
        WHERE (:name IS NULL OR t.name ILIKE %:name%)
          AND (:status IS NULL OR t.status = :status)
          AND (:gender IS NULL OR t.gender = :gender)
          AND (:minAge IS NULL OR t.age >= :minAge)
          AND (:maxAge IS NULL OR t.age <= :maxAge)
          AND t.deleted = false
        ORDER BY t.createdAt DESC
        """)
    Page<TestApi> advancedSearch(
        @Param("name") String name,
        @Param("status") TestApi.TestApiStatus status,
        @Param("gender") TestApi.Gender gender,
        @Param("minAge") Integer minAge,
        @Param("maxAge") Integer maxAge,
        Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM TestApi t WHERE t.status = :status AND t.deleted = false")
    long countByStatus(@Param("status") TestApi.TestApiStatus status);

    @Query("""
        SELECT AVG(t.age) FROM TestApi t
        WHERE t.status = :status AND t.age IS NOT NULL AND t.deleted = false
        """)
    Double averageAgeByStatus(@Param("status") TestApi.TestApiStatus status);

    /* ── @Query — Native SQL ───────────────────────────── */

    @Query(value = """
        SELECT * FROM test_apis
        WHERE deleted = false
          AND (:since IS NULL OR created_at >= :since)
          AND (:status IS NULL OR status = :status)
        ORDER BY created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TestApi> findRecent(
        @Param("since") java.sql.Timestamp since,
        @Param("status") String status,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT gender, COUNT(*) as count
        FROM test_apis
        WHERE deleted = false AND status = :status
        GROUP BY gender
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Object[]> countByGenderGrouped(@Param("status") String status);

    @Query(value = """
        SELECT
            status,
            COUNT(*)        AS total,
            AVG(age)        AS avg_age,
            AVG(score)      AS avg_score,
            MIN(created_at) AS earliest
        FROM test_apis
        WHERE deleted = false
        GROUP BY status
        ORDER BY total DESC
        """, nativeQuery = true)
    List<Object[]> statisticsByStatus();
}
