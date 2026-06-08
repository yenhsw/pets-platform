package com.petsplatform.module.testapi;

import com.petsplatform.module.testapi.domain.TestApi;
import com.petsplatform.module.testapi.dto.TestApiDto;
import com.petsplatform.module.testapi.dto.TestApiMapper;
import com.petsplatform.shared.response.ApiResponse;
import com.petsplatform.shared.response.ResponseHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-apis")
@RequiredArgsConstructor
@Validated
@Tag(name = "TestApi", description = "Demo module — all database patterns")
public class TestApiController {

    private final TestApiService service;
    private final TestApiMapper mapper;

    /* ── CRUD ─────────────────────────────────────────────── */

    @Operation(summary = "Tạo mới TestApi")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo thành công"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email đã tồn tại"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TestApiDto.Response>> create(
            @Valid @RequestBody TestApiDto.CreateRequest request) {
        TestApiDto.Response result = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseHelper.successMsgData("Tạo thành công", result));
    }

    @Operation(summary = "Cập nhật TestApi")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestApiDto.Response>> update(
            @Parameter(description = "UUID") @PathVariable UUID id,
            @Valid @RequestBody TestApiDto.UpdateRequest request) {
        TestApiDto.Response result = service.update(id, request);
        return ResponseEntity.ok(ResponseHelper.successMsgData("Cập nhật thành công", result));
    }

    @Operation(summary = "Xóa mềm TestApi (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDelete(
            @Parameter(description = "UUID") @PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.ok(ResponseHelper.success("Xóa thành công"));
    }

    @Operation(summary = "Tìm TestApi theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestApiDto.Response>> findById(
            @Parameter(description = "UUID") @PathVariable UUID id) {
        TestApiDto.Response r = service.findById(id);
        return ResponseEntity.ok(ResponseHelper.success(r));
    }

    @Operation(summary = "Tìm TestApi theo email")
    @GetMapping("/by-email/{email}")
    public ResponseEntity<ApiResponse<TestApiDto.Response>> findByEmail(
            @Parameter(description = "Email address") @PathVariable String email) {
        TestApiDto.Response r = service.findByEmail(email);
        return ResponseEntity.ok(ResponseHelper.success(r));
    }

    /* ── List — JPA Method Name Queries ──────────────────── */

    @Operation(summary = "List tất cả TestApi (phân trang)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TestApiDto.Response>>> findAll(
            @Parameter(description = "Trang (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số items/trang") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Trường sắp xếp") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "ASC|DESC") @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Page<TestApiDto.Response> result = service.findAll(PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    @Operation(summary = "Tìm theo trạng thái (phân trang)")
    @GetMapping("/by-status/{status}")
    public ResponseEntity<ApiResponse<Page<TestApiDto.Response>>> findByStatus(
            @Parameter(description = "ACTIVE, INACTIVE, SUSPENDED") @PathVariable TestApi.TestApiStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TestApiDto.Response> result = service.findByStatus(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    @Operation(summary = "Tìm theo giới tính")
    @GetMapping("/by-gender/{gender}")
    public ResponseEntity<ApiResponse<List<TestApiDto.Response>>> findByGender(
            @Parameter(description = "MALE, FEMALE, OTHER") @PathVariable TestApi.Gender gender) {
        List<TestApiDto.Response> result = service.findByGender(gender);
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    /* ── Search — @Query JPQL ─────────────────────────── */

    @Operation(summary = "Tìm nâng cao theo nhiều params (JPQL)")
    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<Page<TestApiDto.Response>>> advancedSearch(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) TestApi.TestApiStatus status,
            @RequestParam(required = false) TestApi.Gender gender,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TestApiDto.Response> result = service.advancedSearch(
            name, status, gender, minAge, maxAge, PageRequest.of(page, size));
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    /* ── Dynamic Search — Specification ─────────────────── */

    @Operation(summary = "Tìm động với Specification (tất cả fields)")
    @GetMapping("/search/dynamic")
    public ResponseEntity<ApiResponse<Page<TestApiDto.Response>>> dynamicSearch(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) TestApi.TestApiStatus status,
            @RequestParam(required = false) TestApi.Gender gender,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Double minScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TestApiDto.SearchRequest request = TestApiDto.SearchRequest.builder()
            .name(name).status(status).gender(gender)
            .minAge(minAge).maxAge(maxAge).minScore(minScore)
            .build();
        Page<TestApiDto.Response> result = service.dynamicSearch(request, PageRequest.of(page, size));
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    /* ── Aggregation ─────────────────────────────────────── */

    @Operation(summary = "Đếm số lượng theo trạng thái")
    @GetMapping("/count/by-status/{status}")
    public ResponseEntity<ApiResponse<Long>> countByStatus(
            @PathVariable TestApi.TestApiStatus status) {
        return ResponseEntity.ok(ResponseHelper.success(service.countByStatus(status)));
    }

    @Operation(summary = "Tuổi trung bình theo trạng thái")
    @GetMapping("/avg-age/by-status/{status}")
    public ResponseEntity<ApiResponse<Double>> averageAgeByStatus(
            @PathVariable TestApi.TestApiStatus status) {
        Double avg = service.averageAgeByStatus(status);
        return ResponseEntity.ok(ResponseHelper.success(avg));
    }

    @Operation(summary = "Thống kê theo trạng thái (native SQL)")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<List<TestApiDto.Statistics>>> statisticsByStatus() {
        List<TestApiDto.Statistics> result = service.statisticsByStatus();
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    /* ── Name Query Methods ─────────────────────────────── */

    @Operation(summary = "Tìm theo tên (like)")
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<List<TestApiDto.Response>>> findByNameContaining(
            @RequestParam String name) {
        List<TestApiDto.Response> result = service.findByNameContaining(name);
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    @Operation(summary = "Tìm theo ngày sinh sau một ngày")
    @GetMapping("/birth-after")
    public ResponseEntity<ApiResponse<List<TestApiDto.Response>>> findByBirthDateAfter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TestApiDto.Response> result = service.findByBirthDateAfter(date);
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    @Operation(summary = "Tìm theo điểm tối thiểu")
    @GetMapping("/score-above")
    public ResponseEntity<ApiResponse<List<TestApiDto.Response>>> findByScoreGreaterThanEqual(
            @RequestParam Double minScore) {
        List<TestApiDto.Response> result = service.findByScoreGreaterThanEqual(minScore);
        return ResponseEntity.ok(ResponseHelper.success(result));
    }

    /* ── Native Query ────────────────────────────────────── */

    @Operation(summary = "Tìm user gần đây (native SQL)")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<TestApiDto.Response>>> findRecent(
            @RequestParam(required = false) Instant since,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        List<TestApiDto.Response> result = service.findRecent(since, status, limit);
        return ResponseEntity.ok(ResponseHelper.success(result));
    }
}
