package com.example.demo.application;

import com.example.demo.common.ApiException;
import com.example.demo.company.Company;
import com.example.demo.job.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {
    @Mock JobRepository jobs;
    @Mock ApplicationRepository applications;
    @InjectMocks ApplicationService service;
    Job openJob;
    ApplicationDto.Request request = new ApplicationDto.Request(" An ", " AN@Example.com ", "https://example.com/cv.pdf");

    @BeforeEach
    void setUp() {
        openJob = job(1L, JobStatus.OPEN);
    }
    @Test
    void submitsToOpenJobAndNormalizesEmail() {
        when(jobs.findByIdForUpdate(1L)).thenReturn(Optional.of(openJob));
        when(applications.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.submit(1L, request);
        assertThat(result.email()).isEqualTo("an@example.com");
        assertThat(result.fullName()).isEqualTo("An");
        assertThat(result.jobId()).isEqualTo(1L);
        assertThat(result.appliedAt()).isNotNull();
        verify(applications).saveAndFlush(any(JobApplication.class));
    }
    @Test
    void rejectsMissingJobWithoutSaving() {
        when(jobs.findByIdForUpdate(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(99L, request)).isInstanceOfSatisfying(ApiException.class,
            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(applications);
    }
    @Test
    void rejectsClosedJobWithoutSaving() {
        when(jobs.findByIdForUpdate(1L)).thenReturn(Optional.of(job(1L, JobStatus.CLOSED)));
        assertThatThrownBy(() -> service.submit(1L, request)).isInstanceOfSatisfying(ApiException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("JOB_CLOSED"));
        verifyNoInteractions(applications);
    }
    @Test
    void rejectsDuplicateEmailWithinJob() {
        when(jobs.findByIdForUpdate(1L)).thenReturn(Optional.of(openJob));
        when(applications.existsByJob_IdAndEmail(1L, "an@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.submit(1L, request)).isInstanceOfSatisfying(ApiException.class,
            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(applications, never()).saveAndFlush(any());
    }
    @Test
    void acceptsSameEmailOnAnotherJob() {
        when(jobs.findByIdForUpdate(2L)).thenReturn(Optional.of(job(2L, JobStatus.OPEN)));
        when(applications.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.submit(2L, request).jobId()).isEqualTo(2L);
        verify(applications).existsByJob_IdAndEmail(2L, "an@example.com");
        verify(applications, never()).existsByJob_IdAndEmail(1L, "an@example.com");
    }
    private Job job(Long id, JobStatus status) {
        Job job = new Job(new Company("ACME", "HCM", "https://example.com"),
            new JobDto.Request(1L, "Java", "Backend", BigDecimal.TEN, new BigDecimal("20"), "HCM", status));
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }
}
