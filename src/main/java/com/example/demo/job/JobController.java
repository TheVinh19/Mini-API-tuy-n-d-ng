package com.example.demo.job;

import com.example.demo.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
public class JobController {
    private final JobService service;
    public JobController(JobService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<JobDto.Response> create(@Valid @RequestBody JobDto.Request request) {
        JobDto.Response response = service.create(request);
        return ResponseEntity.created(URI.create("/jobs/" + response.id())).body(response);
    }
    @GetMapping
    public PageResponse<JobDto.Response> search(
        @RequestParam(required = false) @Size(max = 200) String keyword,
        @RequestParam(required = false) @Size(max = 200) String location,
        @RequestParam(required = false) JobStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return service.search(keyword, location, status, page, size, sort);
    }
    @GetMapping("/{id}")
    public JobDto.Response findById(@PathVariable Long id) { return service.findById(id); }
    @PutMapping("/{id}")
    public JobDto.Response update(@PathVariable Long id, @Valid @RequestBody JobDto.Request request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id); return ResponseEntity.noContent().build();
    }
}
