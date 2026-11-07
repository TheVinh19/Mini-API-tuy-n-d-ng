package com.example.demo.company;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
public class CompanyController {
    private final CompanyService service;
    public CompanyController(CompanyService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<CompanyDto.Response> create(@Valid @RequestBody CompanyDto.Request request) {
        CompanyDto.Response response = service.create(request);
        return ResponseEntity.created(URI.create("/companies/" + response.id())).body(response);
    }
    @GetMapping
    public List<CompanyDto.Response> findAll() { return service.findAll(); }
    @GetMapping("/{id}")
    public CompanyDto.Response findById(@PathVariable Long id) { return service.findById(id); }
    @PutMapping("/{id}")
    public CompanyDto.Response update(@PathVariable Long id, @Valid @RequestBody CompanyDto.Request request) {
        return service.update(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
