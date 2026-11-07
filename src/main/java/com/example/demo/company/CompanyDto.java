package com.example.demo.company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
public final class CompanyDto {
    private CompanyDto() { }
    public record Request(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 1000) @URL(regexp = "https?://.*") String website) { }
    public record Response(Long id, String name, String address, String website) {
        public static Response from(Company company) {
            return new Response(company.getId(), company.getName(), company.getAddress(), company.getWebsite());
        }
    }
}
