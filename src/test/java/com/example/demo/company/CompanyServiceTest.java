package com.example.demo.company;

import com.example.demo.common.ApiException;
import com.example.demo.job.JobRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    @Mock CompanyRepository companies;
    @Mock JobRepository jobs;
    @InjectMocks CompanyService service;
    @Test
    void createsTrimmedCompany() {
        when(companies.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.create(new CompanyDto.Request(" ACME ", " HCM ", "https://example.com")).name()).isEqualTo("ACME");
    }
    @Test
    void preventsDeletingCompanyWithJobs() {
        when(companies.findByIdForUpdate(1L)).thenReturn(Optional.of(new Company("ACME", "HCM", "https://example.com")));
        when(jobs.existsByCompany_Id(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L)).isInstanceOfSatisfying(ApiException.class,
            ex -> assertThat(ex.getCode()).isEqualTo("COMPANY_HAS_JOBS"));
        verify(companies, never()).delete(any());
    }
    @Test
    void deletesCompanyWithoutJobs() {
        Company company = new Company("ACME", "HCM", "https://example.com");
        when(companies.findByIdForUpdate(1L)).thenReturn(Optional.of(company));
        service.delete(1L);
        verify(companies).delete(company);
    }
}
