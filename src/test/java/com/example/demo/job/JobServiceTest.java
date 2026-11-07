package com.example.demo.job;

import com.example.demo.application.ApplicationRepository;
import com.example.demo.common.ApiException;
import com.example.demo.company.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {
    @Mock JobRepository jobs;
    @Mock CompanyRepository companies;
    @Mock ApplicationRepository applications;
    @InjectMocks JobService service;
    JobDto.Request request = new JobDto.Request(1L, " Java ", "Backend", BigDecimal.TEN,
        new BigDecimal("20"), "HCM", JobStatus.OPEN);

    @Test
    void createsJobWithExistingCompany() {
        when(companies.findByIdForUpdate(1L)).thenReturn(Optional.of(new Company("ACME", "HCM", "https://example.com")));
        when(jobs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(request).title()).isEqualTo("Java");
        verify(jobs).save(any(Job.class));
    }
    @Test
    void rejectsMissingCompany() {
        when(companies.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request)).isInstanceOfSatisfying(ApiException.class,
            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(jobs);
    }
    @Test
    void rejectsReversedSalaryRange() {
        var invalid = new JobDto.Request(1L, "Java", "Backend", new BigDecimal("30"), BigDecimal.TEN, "HCM", JobStatus.OPEN);
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOfSatisfying(ApiException.class,
            ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(jobs, companies);
    }
    @Test
    void rejectsDeletingJobWithApplications() {
        when(jobs.findByIdForUpdate(1L)).thenReturn(Optional.of(new Job(new Company("ACME", "HCM", "https://example.com"), request)));
        when(applications.existsByJob_Id(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOfSatisfying(ApiException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("JOB_HAS_APPLICATIONS"));
        verify(jobs, never()).delete(any(Job.class));
    }
    @Test
    void rejectsInvalidPaginationAndSortBeforeQuery() {
        assertThatThrownBy(() -> service.search(null, null, null, -1, 10, "id,asc")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.search(null, null, null, 0, 101, "id,asc")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.search(null, null, null, 0, 10, "unknown,asc")).isInstanceOf(ApiException.class);
        verifyNoInteractions(jobs);
    }
}
