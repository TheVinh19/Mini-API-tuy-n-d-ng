package com.example.demo.application;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByJob_IdAndEmail(Long jobId, String email);
    boolean existsByJob_Id(Long jobId);
}
