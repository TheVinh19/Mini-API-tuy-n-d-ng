package com.example.demo.company;
import com.example.demo.common.ApiException;
import com.example.demo.job.JobRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {
    private final CompanyRepository companies;
    private final JobRepository jobs;
    public CompanyService(CompanyRepository companies, JobRepository jobs) {
        this.companies = companies; this.jobs = jobs;
    }
    public List<CompanyDto.Response> findAll() {
        return companies.findAll(Sort.by("id")).stream().map(CompanyDto.Response::from).toList();
    }
    public CompanyDto.Response findById(Long id) {
        return CompanyDto.Response.from(companies.findById(id).orElseThrow(() -> ApiException.notFound("Company", id)));
    }
    @Transactional
    public CompanyDto.Response create(CompanyDto.Request request) {
        return CompanyDto.Response.from(companies.save(new Company(request.name().trim(),
            request.address().trim(), request.website().trim())));
    }
    @Transactional
    public CompanyDto.Response update(Long id, CompanyDto.Request request) {
        Company company = companies.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Company", id));
        company.update(request.name().trim(), request.address().trim(), request.website().trim());
        return CompanyDto.Response.from(companies.save(company));
    }
    @Transactional
    public void delete(Long id) {
        Company company = companies.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Company", id));
        if (jobs.existsByCompany_Id(id)) {
            throw ApiException.conflict("COMPANY_HAS_JOBS", "Không thể xóa công ty đang có tin tuyển dụng");
        }
        companies.delete(company);
    }
}
