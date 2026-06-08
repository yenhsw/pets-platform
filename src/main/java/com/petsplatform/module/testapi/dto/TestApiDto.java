package com.petsplatform.module.testapi.dto;

import com.petsplatform.module.testapi.domain.TestApi;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TestApiDto {

    private TestApiDto() {}

    /* ══════════════════════════════════════════════════════════
       CREATE / UPDATE REQUEST
       ══════════════════════════════════════════════════════════ */

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "Tên không được để trống")
        @Size(min = 2, max = 100, message = "Tên phải từ 2 đến 100 ký tự")
        private String name;

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        private String phone;

        @Min(value = 1, message = "Tuổi phải lớn hơn 0")
        @Max(value = 150, message = "Tuổi không hợp lệ")
        private Integer age;

        private TestApi.Gender gender;

        @DecimalMin(value = "0.0", message = "Lương không được âm")
        private BigDecimal salary;

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        private LocalDate birthDate;

        @Size(max = 500, message = "Bio tối đa 500 ký tự")
        private String bio;

        @NotNull(message = "Trạng thái không được để trống")
        private TestApi.TestApiStatus status;

        @DecimalMin(value = "0.0", message = "Điểm không được âm")
        @DecimalMax(value = "10.0", message = "Điểm tối đa là 10")
        private Double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @Size(min = 2, max = 100, message = "Tên phải từ 2 đến 100 ký tự")
        private String name;

        @Email(message = "Email không hợp lệ")
        private String email;

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        private String phone;

        @Min(value = 1, message = "Tuổi phải lớn hơn 0")
        @Max(value = 150, message = "Tuổi không hợp lệ")
        private Integer age;

        private TestApi.Gender gender;

        @DecimalMin(value = "0.0", message = "Lương không được âm")
        private BigDecimal salary;

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        private LocalDate birthDate;

        @Size(max = 500, message = "Bio tối đa 500 ký tự")
        private String bio;

        private TestApi.TestApiStatus status;

        @DecimalMin(value = "0.0", message = "Điểm không được âm")
        @DecimalMax(value = "10.0", message = "Điểm tối đa là 10")
        private Double score;
    }

    /* ══════════════════════════════════════════════════════════
       SEARCH / FILTER REQUEST
       ══════════════════════════════════════════════════════════ */

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {

        private String name;
        private String email;
        private String phone;
        private TestApi.TestApiStatus status;
        private TestApi.Gender gender;
        private Integer minAge;
        private Integer maxAge;
        private Double minScore;
    }

    /* ══════════════════════════════════════════════════════════
       RESPONSE
       ══════════════════════════════════════════════════════════ */

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private Integer age;
        private TestApi.Gender gender;
        private BigDecimal salary;
        private LocalDate birthDate;
        private String bio;
        private TestApi.TestApiStatus status;
        private Double score;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private String status;
        private Long total;
        private Double avgAge;
        private Double avgScore;
        private Instant earliestCreated;
    }
}
