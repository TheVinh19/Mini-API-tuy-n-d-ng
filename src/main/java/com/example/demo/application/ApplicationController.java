package com.example.demo.application;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs/{jobId}/applications")
public class ApplicationController {
    private final ApplicationService service;
    public ApplicationController(ApplicationService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto.Response submit(@PathVariable Long jobId, @Valid @RequestBody ApplicationDto.Request request) {
        return service.submit(jobId, request);
    }
}
