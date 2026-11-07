package com.example.demo.job;

import com.example.demo.application.ApplicationRepository;
import com.example.demo.common.ApiException;
import com.example.demo.common.PageResponse;
import com.example.demo.company.Company;
import com.example.demo.company.CompanyRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "title", "salaryFrom", "salaryTo", "location", "status", "createdAt");
    private final JobRepository jobs;
    private final CompanyRepository companies;
    private final ApplicationRepository applications;
    public JobService(JobRepository jobs, CompanyRepository companies, ApplicationRepository applications) {
        this.jobs = jobs; this.companies = companies; this.applications = applications;
    }
    public JobDto.Response findById(Long id) {
        return JobDto.Response.from(jobs.findById(id).orElseThrow(() -> ApiException.notFound("Job", id)));
    }
    public PageResponse<JobDto.Response> search(String keyword, String location, JobStatus status,
            int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("page phải >= 0; size phải nằm trong khoảng 1–100");
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0]) ||
            !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw ApiException.badRequest("sort phải có dạng field,asc hoặc field,desc với trường được hỗ trợ");
        }
        Sort ordering = Sort.by(Sort.Direction.valueOf(parts[1].toUpperCase(Locale.ROOT)), parts[0]);
        if (!parts[0].equals("id")) { ordering = ordering.and(Sort.by("id")); }
        return PageResponse.from(jobs.findAll(JobSpecifications.filter(keyword, location, status),
            PageRequest.of(page, size, ordering)).map(JobDto.Response::from));
    }
    @Transactional
    public JobDto.Response create(JobDto.Request request) {
        validateSalary(request);
        Company company = lockCompany(request.companyId());
        return JobDto.Response.from(jobs.save(new Job(company, request)));
    }
    @Transactional
    public JobDto.Response update(Long id, JobDto.Request request) {
        validateSalary(request);
        Job job = jobs.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Job", id));
        job.update(lockCompany(request.companyId()), request);
        return JobDto.Response.from(jobs.save(job));
    }
    @Transactional
    public void delete(Long id) {
        Job job = jobs.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Job", id));
        if (applications.existsByJob_Id(id)) {
            throw ApiException.conflict("JOB_HAS_APPLICATIONS", "Không thể xóa Job đã có hồ sơ ứng tuyển");
        }
        jobs.delete(job);
    }
    private Company lockCompany(Long id) {
        return companies.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Company", id));
    }
    private void validateSalary(JobDto.Request request) {
        if (request.salaryFrom() == null || request.salaryTo() == null || request.salaryFrom().signum() < 0 ||
            request.salaryFrom().compareTo(request.salaryTo()) > 0) {
            throw ApiException.badRequest("Khoảng lương không hợp lệ");
        }
    }
}
