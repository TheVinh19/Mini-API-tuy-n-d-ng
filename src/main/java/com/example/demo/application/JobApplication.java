package com.example.demo.application;

import com.example.demo.job.Job;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(
    name = "uk_applications_job_email", columnNames = {"job_id", "email"}))
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    @Column(nullable = false, length = 200)
    private String fullName;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false, length = 2000)
    private String cvUrl;
    @Column(nullable = false, updatable = false)
    private Instant appliedAt;
    protected JobApplication() { }
    public JobApplication(Job job, String fullName, String email, String cvUrl) {
        this.job = job; this.fullName = fullName; this.email = email; this.cvUrl = cvUrl;
        this.appliedAt = Instant.now();
    }
    public Long getId() { return id; }
    public Job getJob() { return job; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getCvUrl() { return cvUrl; }
    public Instant getAppliedAt() { return appliedAt; }
}
