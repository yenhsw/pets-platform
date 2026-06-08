package com.petsplatform.module.testapi.dto;

import com.petsplatform.module.testapi.domain.TestApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public final class TestApiMapper {

    public TestApi toEntity(TestApiDto.CreateRequest dto) {
        return TestApi.builder()
            .name(dto.getName())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .age(dto.getAge())
            .gender(dto.getGender())
            .salary(dto.getSalary())
            .birthDate(dto.getBirthDate())
            .bio(dto.getBio())
            .status(dto.getStatus())
            .score(dto.getScore())
            .build();
    }

    public void updateEntity(TestApi entity, TestApiDto.UpdateRequest dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getAge() != null) entity.setAge(dto.getAge());
        if (dto.getGender() != null) entity.setGender(dto.getGender());
        if (dto.getSalary() != null) entity.setSalary(dto.getSalary());
        if (dto.getBirthDate() != null) entity.setBirthDate(dto.getBirthDate());
        if (dto.getBio() != null) entity.setBio(dto.getBio());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getScore() != null) entity.setScore(dto.getScore());
    }

    public TestApiDto.Response toResponse(TestApi entity) {
        if (entity == null) return null;
        return TestApiDto.Response.builder()
            .id(entity.getId())
            .name(entity.getName())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .age(entity.getAge())
            .gender(entity.getGender())
            .salary(entity.getSalary())
            .birthDate(entity.getBirthDate())
            .bio(entity.getBio())
            .status(entity.getStatus())
            .score(entity.getScore())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    public List<TestApiDto.Response> toResponseList(List<TestApi> entities) {
        return entities.stream().map(this::toResponse).toList();
    }

    public Page<TestApiDto.Response> toResponsePage(Page<TestApi> page) {
        return page.map(this::toResponse);
    }
}
