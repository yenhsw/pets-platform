package com.petsplatform.module.testapi.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TestApiSpecifications {

    private TestApiSpecifications() {}

    public static Specification<TestApi> withName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }

    public static Specification<TestApi> withEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) return null;
            return cb.equal(cb.lower(root.get("email")), email.toLowerCase());
        };
    }

    public static Specification<TestApi> withPhone(String phone) {
        return (root, query, cb) -> {
            if (phone == null || phone.isBlank()) return null;
            return cb.like(root.get("phone"), "%" + phone + "%");
        };
    }

    public static Specification<TestApi> withStatus(TestApi.TestApiStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<TestApi> withGender(TestApi.Gender gender) {
        return (root, query, cb) -> {
            if (gender == null) return null;
            return cb.equal(root.get("gender"), gender);
        };
    }

    public static Specification<TestApi> withMinAge(Integer minAge) {
        return (root, query, cb) -> {
            if (minAge == null) return null;
            return cb.greaterThanOrEqualTo(root.get("age"), minAge);
        };
    }

    public static Specification<TestApi> withMaxAge(Integer maxAge) {
        return (root, query, cb) -> {
            if (maxAge == null) return null;
            return cb.lessThanOrEqualTo(root.get("age"), maxAge);
        };
    }

    public static Specification<TestApi> withMinScore(Double minScore) {
        return (root, query, cb) -> {
            if (minScore == null) return null;
            return cb.greaterThanOrEqualTo(root.get("score"), minScore);
        };
    }

    public static Specification<TestApi> activeOnly() {
        return (root, query, cb) -> cb.equal(root.get("status"), TestApi.TestApiStatus.ACTIVE);
    }

    public static Specification<TestApi> inactiveOnly() {
        return (root, query, cb) -> cb.equal(root.get("status"), TestApi.TestApiStatus.INACTIVE);
    }

    public static Specification<TestApi> withBirthDateBefore(java.time.LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            return cb.lessThan(root.get("birthDate"), date);
        };
    }

    public static Specification<TestApi> withBirthDateAfter(java.time.LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return null;
            return cb.greaterThan(root.get("birthDate"), date);
        };
    }

    /**
     * Combines multiple specifications with AND logic, ignoring null ones.
     *
     * <p>Usage:
     * <pre>
     * Specification&lt;TestApi&gt; spec = TestApiSpecifications
     *     .withName("John")
     *     .and(TestApiSpecifications.withStatus(ACTIVE))
     *     .and(TestApiSpecifications.withMinAge(18));
     * </pre>
     */
    public static Specification<TestApi> combine(
            String name,
            TestApi.TestApiStatus status,
            TestApi.Gender gender,
            Integer minAge,
            Integer maxAge,
            Double minScore
    ) {
        Specification<TestApi> spec = null;
        List<Specification<TestApi>> specs = List.of(
            withName(name),
            withStatus(status),
            withGender(gender),
            withMinAge(minAge),
            withMaxAge(maxAge),
            withMinScore(minScore)
        );
        for (Specification<TestApi> s : specs) {
            if (s != null) {
                spec = (spec == null) ? s : spec.and(s);
            }
        }
        return spec;
    }
}
