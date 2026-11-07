package com.example.demo.job;

import com.example.demo.company.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "jobs")
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal salaryFrom;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal salaryTo;
    @Column(nullable = false, length = 200)
    private String location;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private JobStatus status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    protected Job() { }
    public Job(Company company, JobDto.Request request) {
        update(company, request); this.createdAt = Instant.now();
    }
    public void update(Company company, JobDto.Request request) {
        this.company = company; this.title = request.title().trim();
        this.description = request.description().trim();
        this.salaryFrom = request.salaryFrom(); this.salaryTo = request.salaryTo();
        this.location = request.location().trim(); this.status = request.status();
    }
    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getSalaryFrom() { return salaryFrom; }
    public BigDecimal getSalaryTo() { return salaryTo; }
    public String getLocation() { return location; }
    public JobStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
