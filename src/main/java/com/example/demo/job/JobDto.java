package com.example.demo.job;

import com.example.demo.company.CompanyDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class JobDto {
    private JobDto() { }
    public record Request(
        @NotNull @Positive Long companyId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20000) String description,
        @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal salaryFrom,
        @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal salaryTo,
        @NotBlank @Size(max = 200) String location,
        @NotNull JobStatus status) {
        @JsonIgnore
        @AssertTrue(message = "Lương từ phải nhỏ hơn hoặc bằng lương đến")
        public boolean isSalaryRangeValid() {
            return salaryFrom == null || salaryTo == null || salaryFrom.compareTo(salaryTo) <= 0;
        }
    }
    public record Response(Long id, String title, String description, BigDecimal salaryFrom,
        BigDecimal salaryTo, String location, JobStatus status, Instant createdAt, CompanyDto.Response company) {
        public static Response from(Job job) {
            return new Response(job.getId(), job.getTitle(), job.getDescription(), job.getSalaryFrom(),
                job.getSalaryTo(), job.getLocation(), job.getStatus(), job.getCreatedAt(),
                CompanyDto.Response.from(job.getCompany()));
        }
    }
}
