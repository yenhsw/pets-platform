package com.petsplatform.module.testapi;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import com.petsplatform.module.testapi.domain.TestApi;
import com.petsplatform.module.testapi.domain.TestApiRepository;
import com.petsplatform.module.testapi.dto.TestApiDto;
import com.petsplatform.module.testapi.dto.TestApiMapper;
import com.petsplatform.shared.base.AuditorContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureDataJpa
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestApiRepositoryTest {

    @Autowired
    private TestApiRepository repository;

    @Autowired
    private EntityManager entityManager;

    private static UUID savedId;

    @BeforeEach
    void setUp() {
        AuditorContext.setAuditor("test-user");
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        AuditorContext.clear();
    }

    /* ══════════════════════════════════════════════════════════
       JPA — Method Name Queries
       ══════════════════════════════════════════════════════════ */

    @Test
    @Order(1)
    void save_sets_uuid_and_auditing_fields() {
        TestApi entity = TestApi.builder()
            .name("Nguyen Van A")
            .email("test1@example.com")
            .status(TestApi.TestApiStatus.ACTIVE)
            .age(25)
            .build();

        TestApi saved = repository.save(entity);
        savedId = saved.getId();

        assertThat(savedId).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("test-user");
        assertThat(saved.getUpdatedBy()).isEqualTo("test-user");
    }

    @Test
    @Order(2)
    void findByEmail_method_name_query() {
        repository.save(TestApi.builder().name("Test").email("findby@example.com")
            .status(TestApi.TestApiStatus.ACTIVE).build());
        entityManager.flush();

        Optional<TestApi> found = repository.findByEmail("findby@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test");
    }

    @Test
    @Order(3)
    void existsByEmail_method_name_query() {
        repository.save(TestApi.builder().name("Test").email("exists@example.com")
            .status(TestApi.TestApiStatus.ACTIVE).build());
        entityManager.flush();

        assertThat(repository.existsByEmail("exists@example.com")).isTrue();
        assertThat(repository.existsByEmail("notfound@example.com")).isFalse();
    }

    @Test
    @Order(4)
    void findByStatus_pageable() {
        saveAll(
            status(TestApi.TestApiStatus.ACTIVE),
            status(TestApi.TestApiStatus.ACTIVE),
            status(TestApi.TestApiStatus.INACTIVE)
        );
        entityManager.flush();

        Page<TestApi> page = repository.findByStatus(
            TestApi.TestApiStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(e -> e.getStatus() == TestApi.TestApiStatus.ACTIVE);
    }

    @Test
    @Order(5)
    void findByGender_list() {
        saveAll(
            gender(TestApi.Gender.MALE),
            gender(TestApi.Gender.MALE),
            gender(TestApi.Gender.FEMALE)
        );
        entityManager.flush();

        List<TestApi> males = repository.findByGender(TestApi.Gender.MALE);
        assertThat(males).hasSize(2);
        assertThat(males).allMatch(e -> e.getGender() == TestApi.Gender.MALE);
    }

    @Test
    @Order(6)
    void findByAgeBetween() {
        saveAll(age(20), age(25), age(30), age(35));
        entityManager.flush();

        List<TestApi> between = repository.findByAgeBetween(22, 32);
        assertThat(between).hasSize(2);
        assertThat(between).allMatch(e -> e.getAge() >= 22 && e.getAge() <= 32);
    }

    /* ══════════════════════════════════════════════════════════
       JPA — @Query JPQL
       ══════════════════════════════════════════════════════════ */

    @Test
    @Order(10)
    void searchByNameAndStatus_jpql() {
        saveAll(
            TestApi.builder().name("Nguyen A").status(TestApi.TestApiStatus.ACTIVE).build(),
            TestApi.builder().name("Nguyen B").status(TestApi.TestApiStatus.ACTIVE).build(),
            TestApi.builder().name("Tran C").status(TestApi.TestApiStatus.INACTIVE).build()
        );
        entityManager.flush();

        List<TestApi> results = repository.searchByNameAndStatus("Nguyen", TestApi.TestApiStatus.ACTIVE);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(e -> e.getName().contains("Nguyen"));
    }

    @Test
    @Order(11)
    void advancedSearch_jpql_null_params() {
        saveAll(status(TestApi.TestApiStatus.ACTIVE), status(TestApi.TestApiStatus.INACTIVE));
        entityManager.flush();

        Page<TestApi> all = repository.advancedSearch(
            null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(all.getTotalElements()).isEqualTo(2);

        Page<TestApi> filtered = repository.advancedSearch(
            null, TestApi.TestApiStatus.ACTIVE, null, null, null, PageRequest.of(0, 10));
        assertThat(filtered.getTotalElements()).isEqualTo(1);
    }

    @Test
    @Order(12)
    void countByStatus_jpql() {
        saveAll(status(TestApi.TestApiStatus.ACTIVE), status(TestApi.TestApiStatus.ACTIVE),
            status(TestApi.TestApiStatus.INACTIVE));
        entityManager.flush();

        assertThat(repository.countByStatus(TestApi.TestApiStatus.ACTIVE)).isEqualTo(2);
        assertThat(repository.countByStatus(TestApi.TestApiStatus.INACTIVE)).isEqualTo(1);
    }

    @Test
    @Order(13)
    void averageAgeByStatus_jpql() {
        TestApi e1 = TestApi.builder().name("A").status(TestApi.TestApiStatus.ACTIVE).age(20).build();
        TestApi e2 = TestApi.builder().name("B").status(TestApi.TestApiStatus.ACTIVE).age(30).build();
        repository.saveAll(List.of(e1, e2));
        entityManager.flush();

        Double avg = repository.averageAgeByStatus(TestApi.TestApiStatus.ACTIVE);
        assertThat(avg).isEqualTo(25.0);
    }

    /* ══════════════════════════════════════════════════════════
       JPA — @Query Native SQL
       ══════════════════════════════════════════════════════════ */

    @Test
    @Order(20)
    void findRecent_native_query() {
        TestApi recent = TestApi.builder().name("Recent").email("recent@example.com")
            .status(TestApi.TestApiStatus.ACTIVE).build();
        repository.save(recent);
        entityManager.flush();

        List<TestApi> results = repository.findRecent(null, null, 5);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getName()).isNotNull();
    }

    @Test
    @Order(21)
    void statisticsByStatus_native_query() {
        saveAll(status(TestApi.TestApiStatus.ACTIVE), status(TestApi.TestApiStatus.ACTIVE),
            status(TestApi.TestApiStatus.INACTIVE));
        entityManager.flush();

        List<Object[]> stats = repository.statisticsByStatus();
        assertThat(stats).isNotEmpty();
    }

    /* ══════════════════════════════════════════════════════════
       Soft Delete
       ══════════════════════════════════════════════════════════ */

    @Test
    @Order(30)
    void softDelete_excludes_from_findById() {
        TestApi entity = repository.save(TestApi.builder()
            .name("ToDelete").email("delete@example.com")
            .status(TestApi.TestApiStatus.ACTIVE).build());
        entityManager.flush();

        entity.markDeleted();
        repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        Optional<TestApi> found = repository.findById(entity.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @Order(31)
    void findAll_excludes_soft_deleted() {
        TestApi active = repository.save(TestApi.builder()
            .name("Active").email("active@example.com")
            .status(TestApi.TestApiStatus.ACTIVE).build());
        TestApi deleted = repository.save(TestApi.builder()
            .name("Deleted").email("deleted@example.com")
            .status(TestApi.TestApiStatus.ACTIVE).build());
        entityManager.flush();

        deleted.markDeleted();
        repository.save(deleted);
        entityManager.flush();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll().get(0).getName()).isEqualTo("Active");
    }

    /* ══════════════════════════════════════════════════════════
       Pagination
       ══════════════════════════════════════════════════════════ */

    @Test
    @Order(40)
    void findAll_pagination() {
        for (int i = 0; i < 25; i++) {
            repository.save(TestApi.builder().name("User" + i).email("user" + i + "@x.com")
                .status(TestApi.TestApiStatus.ACTIVE).build());
        }
        entityManager.flush();

        Page<TestApi> page1 = repository.findAll(PageRequest.of(0, 10));
        Page<TestApi> page2 = repository.findAll(PageRequest.of(1, 10));
        Page<TestApi> page3 = repository.findAll(PageRequest.of(2, 10));

        assertThat(page1.getTotalElements()).isEqualTo(25);
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(10);
        assertThat(page3.getContent()).hasSize(5);
    }

    /* ══════════════════════════════════════════════════════════
       Helper
       ══════════════════════════════════════════════════════════ */

    private TestApi status(TestApi.TestApiStatus s) {
        return TestApi.builder().name("Test").email(UUID.randomUUID() + "@x.com").status(s).build();
    }

    private TestApi gender(TestApi.Gender g) {
        return TestApi.builder().name("Test").email(UUID.randomUUID() + "@x.com")
            .gender(g).status(TestApi.TestApiStatus.ACTIVE).build();
    }

    private TestApi age(int a) {
        return TestApi.builder().name("Test").email(UUID.randomUUID() + "@x.com")
            .age(a).status(TestApi.TestApiStatus.ACTIVE).build();
    }

    private void saveAll(TestApi... entities) {
        repository.saveAll(List.of(entities));
    }
}
