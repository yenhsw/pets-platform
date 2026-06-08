package com.petsplatform.module.testapi;

import com.petsplatform.module.testapi.domain.TestApi;
import com.petsplatform.module.testapi.domain.TestApiRepository;
import com.petsplatform.module.testapi.domain.TestApiSpecifications;
import com.petsplatform.module.testapi.dto.TestApiDto;
import com.petsplatform.module.testapi.dto.TestApiMapper;
import com.petsplatform.shared.base.AuditorContext;
import com.petsplatform.shared.exception.ResourceConflictException;
import com.petsplatform.shared.exception.ResourceNotFoundException;
import com.petsplatform.shared.logging.MdcUtils;
import com.petsplatform.shared.validation.ValidationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestApiService {

    private final TestApiRepository repository;
    private final TestApiMapper mapper;
    private final ValidationHelper validationHelper;

    private void setAuditor() {
        String userId = MdcUtils.get(MdcUtils.USER_ID);
        AuditorContext.setAuditor(userId != null ? userId : "api-user");
    }

    /* ── CRUD — JPA Method Name Queries ─────────────────── */

    @Transactional
    public TestApiDto.Response create(TestApiDto.CreateRequest request) {
        setAuditor();
        try {
            validationHelper.validate(request);

            if (repository.existsByEmail(request.getEmail())) {
                throw new ResourceConflictException("TestApi", "email", request.getEmail());
            }

            TestApi entity = mapper.toEntity(request);
            return mapper.toResponse(repository.save(entity));
        } finally {
            AuditorContext.clear();
        }
    }

    @Transactional
    public TestApiDto.Response update(UUID id, TestApiDto.UpdateRequest request) {
        setAuditor();
        try {
            TestApi entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestApi", "id", id.toString()));

            if (request.getEmail() != null && !request.getEmail().equals(entity.getEmail())) {
                repository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResourceConflictException("TestApi", "email", request.getEmail());
                    });
            }

            mapper.updateEntity(entity, request);
            return mapper.toResponse(repository.save(entity));
        } finally {
            AuditorContext.clear();
        }
    }

    @Transactional
    public void softDelete(UUID id) {
        setAuditor();
        try {
            TestApi entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestApi", "id", id.toString()));
            entity.markDeleted();
            repository.save(entity);
        } finally {
            AuditorContext.clear();
        }
    }

    public TestApiDto.Response findById(UUID id) {
        return mapper.toResponse(repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TestApi", "id", id.toString())));
    }

    public TestApiDto.Response findByEmail(String email) {
        return mapper.toResponse(repository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("TestApi", "email", email)));
    }

    /* ── List — JPA Method Name + Pagination ──────────── */

    public Page<TestApiDto.Response> findAll(Pageable pageable) {
        return mapper.toResponsePage(repository.findAll(pageable));
    }

    public Page<TestApiDto.Response> findByStatus(TestApi.TestApiStatus status, Pageable pageable) {
        return mapper.toResponsePage(repository.findByStatus(status, pageable));
    }

    public List<TestApiDto.Response> findByGender(TestApi.Gender gender) {
        return mapper.toResponseList(repository.findByGender(gender));
    }

    public List<TestApiDto.Response> findByAgeBetween(Integer minAge, Integer maxAge) {
        return mapper.toResponseList(repository.findByAgeBetween(minAge, maxAge));
    }

    /* ── Search — @Query JPQL ─────────────────────────── */

    public List<TestApiDto.Response> searchByNameAndStatus(String name, TestApi.TestApiStatus status) {
        return mapper.toResponseList(repository.searchByNameAndStatus(name, status));
    }

    public Page<TestApiDto.Response> advancedSearch(
            String name, TestApi.TestApiStatus status, TestApi.Gender gender,
            Integer minAge, Integer maxAge, Pageable pageable) {
        return mapper.toResponsePage(repository.advancedSearch(name, status, gender, minAge, maxAge, pageable));
    }

    /* ── Aggregation — @Query ────────────────────────── */

    public long countByStatus(TestApi.TestApiStatus status) {
        return repository.countByStatus(status);
    }

    public Double averageAgeByStatus(TestApi.TestApiStatus status) {
        return repository.averageAgeByStatus(status);
    }

    /* ── Dynamic Search — Specification ──────────────── */

    public Page<TestApiDto.Response> dynamicSearch(TestApiDto.SearchRequest request, Pageable pageable) {
        var spec = TestApiSpecifications.combine(
            request.getName(), request.getStatus(), request.getGender(),
            request.getMinAge(), request.getMaxAge(), request.getMinScore()
        );
        return mapper.toResponsePage(repository.findAll(spec, pageable));
    }

    /* ── Native Query ──────────────────────────────── */

    public List<TestApiDto.Response> findRecent(Instant since, String status, int limit) {
        Timestamp sqlDate = since != null ? Timestamp.from(since) : null;
        return mapper.toResponseList(repository.findRecent(sqlDate, status, limit));
    }

    public List<TestApiDto.Statistics> statisticsByStatus() {
        return repository.statisticsByStatus().stream().map(row -> {
            TestApiDto.Statistics s = new TestApiDto.Statistics();
            s.setStatus(row[0] != null ? row[0].toString() : null);
            s.setTotal(row[1] != null ? ((Number) row[1]).longValue() : 0L);
            s.setAvgAge(row[2] != null ? ((Number) row[2]).doubleValue() : null);
            s.setAvgScore(row[3] != null ? ((Number) row[3]).doubleValue() : null);
            s.setEarliestCreated(row[4] != null ? ((Timestamp) row[4]).toInstant() : null);
            return s;
        }).toList();
    }

    /* ── Name Query Methods ──────────────────────────── */

    public List<TestApiDto.Response> findByNameContaining(String name) {
        return mapper.toResponseList(repository.findByNameContainingIgnoreCase(name));
    }

    public List<TestApiDto.Response> findByBirthDateAfter(LocalDate date) {
        return mapper.toResponseList(repository.findByBirthDateAfter(date));
    }

    public List<TestApiDto.Response> findByScoreGreaterThanEqual(Double minScore) {
        return mapper.toResponseList(repository.findByScoreGreaterThanEqual(minScore));
    }
}
