package com.example.demo.application;

import com.example.demo.common.ApiException;
import com.example.demo.job.Job;
import com.example.demo.job.JobRepository;
import com.example.demo.job.JobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    private final JobRepository jobs;
    private final ApplicationRepository applications;
    public ApplicationService(JobRepository jobs, ApplicationRepository applications) {
        this.jobs = jobs; this.applications = applications;
    }
    @Transactional
    public ApplicationDto.Response submit(Long jobId, ApplicationDto.Request request) {
        // Shared with Job update/delete: closing a Job cannot race an application submission.
        Job job = jobs.findByIdForUpdate(jobId).orElseThrow(() -> ApiException.notFound("Job", jobId));
        if (job.getStatus() == JobStatus.CLOSED) {
            throw ApiException.conflict("JOB_CLOSED", "Tin tuyển dụng đã đóng");
        }
        if (applications.existsByJob_IdAndEmail(jobId, request.email())) {
            throw ApiException.conflict("DUPLICATE_APPLICATION", "Email đã nộp hồ sơ vào Job này");
        }
        return ApplicationDto.Response.from(applications.saveAndFlush(new JobApplication(job,
            request.fullName().trim(), request.email(), request.cvUrl().trim())));
    }
}
