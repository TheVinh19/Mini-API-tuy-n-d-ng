package com.example.demo;

import com.example.demo.application.ApplicationRepository;
import com.example.demo.company.CompanyRepository;
import com.example.demo.job.JobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ApplicationRepository applications;
    @Autowired JobRepository jobs;
    @Autowired CompanyRepository companies;

    @BeforeEach
    void cleanDatabase() {
        applications.deleteAllInBatch(); jobs.deleteAllInBatch(); companies.deleteAllInBatch();
    }

    @Test
    void browserGetToApplicationEndpointReturnsJsonAndAllowedMethod() throws Exception {
        mvc.perform(get("/jobs/1/applications").accept(MediaType.TEXT_HTML))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", "POST"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(405))
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
            .andExpect(jsonPath("$.path").value("/jobs/1/applications"))
            .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void companyCrudLifecycle() throws Exception {
        long id = createCompany();
        mvc.perform(get("/companies/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("ACME"));
        mvc.perform(get("/companies")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/companies/{id}", id).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("name", "Updated", "address", "Hanoi", "website", "https://example.com"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
        mvc.perform(delete("/companies/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/companies/{id}", id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void jobCrudAndRestrictedCompanyDelete() throws Exception {
        long companyId = createCompany();
        long jobId = createJob(companyId, "Java Developer", "HCM", "OPEN", 10);
        mvc.perform(delete("/companies/{id}", companyId)).andExpect(status().isConflict());
        mvc.perform(get("/jobs/{id}", jobId)).andExpect(status().isOk()).andExpect(jsonPath("$.company.id").value(companyId));
        mvc.perform(put("/jobs/{id}", jobId).contentType(MediaType.APPLICATION_JSON)
            .content(jobBody(companyId, "Updated Java", "Hanoi", "CLOSED", 10)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(delete("/jobs/{id}", jobId)).andExpect(status().isNoContent());
        mvc.perform(get("/jobs/{id}", jobId)).andExpect(status().isNotFound());
        mvc.perform(delete("/companies/{id}", companyId)).andExpect(status().isNoContent());
    }

    @Test
    void combinesCaseInsensitiveFiltersPaginationAndSorting() throws Exception {
        long companyId = createCompany();
        createJob(companyId, "Java Senior", "HCM", "OPEN", 30);
        createJob(companyId, "Java Junior", "HCM", "OPEN", 10);
        createJob(companyId, "Java Closed", "HCM", "CLOSED", 10);
        createJob(companyId, "Java Hanoi", "Hanoi", "OPEN", 10);
        mvc.perform(get("/jobs").param("keyword", "JAVA").param("location", "hcm")
            .param("status", "OPEN").param("page", "0").param("size", "1").param("sort", "salaryFrom,asc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Java Junior"));
        mvc.perform(get("/jobs").param("keyword", "java").param("location", "HCM")
            .param("status", "OPEN").param("page", "1").param("size", "1").param("sort", "salaryFrom,asc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].title").value("Java Senior"));
    }

    @Test
    void treatsSqlLikeWildcardsAsLiteralSearchText() throws Exception {
        long companyId = createCompany();
        createJob(companyId, "100% Java", "HCM", "OPEN", 10);
        createJob(companyId, "Regular Java", "HCM", "OPEN", 10);
        mvc.perform(get("/jobs").param("keyword", "%")).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].title").value("100% Java"));
    }

    @Test
    void applicationRulesAndDeleteRestriction() throws Exception {
        long companyId = createCompany();
        long first = createJob(companyId, "Java", "HCM", "OPEN", 10);
        long second = createJob(companyId, "Python", "HCM", "OPEN", 10);
        mvc.perform(post("/jobs/{id}/applications", first).contentType(MediaType.APPLICATION_JSON)
            .content(applicationBody(" AN@Example.com "))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("an@example.com")).andExpect(jsonPath("$.appliedAt").exists());
        mvc.perform(post("/jobs/{id}/applications", first).contentType(MediaType.APPLICATION_JSON)
            .content(applicationBody("an@example.com"))).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_APPLICATION"));
        mvc.perform(post("/jobs/{id}/applications", second).contentType(MediaType.APPLICATION_JSON)
            .content(applicationBody("an@example.com"))).andExpect(status().isCreated());
        mvc.perform(delete("/jobs/{id}", first)).andExpect(status().isConflict());
        mvc.perform(put("/jobs/{id}", first).contentType(MediaType.APPLICATION_JSON)
            .content(jobBody(companyId, "Java", "HCM", "CLOSED", 10))).andExpect(status().isOk());
        mvc.perform(post("/jobs/{id}/applications", first).contentType(MediaType.APPLICATION_JSON)
            .content(applicationBody("new@example.com"))).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("JOB_CLOSED"));
        assertThat(applications.count()).isEqualTo(2);
    }

    @Test
    void concurrentSubmissionsOnlyCreateOneApplication() throws Exception {
        long jobId = createJob(createCompany(), "Java", "HCM", "OPEN", 10);
        String body = applicationBody("same@example.com");
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Integer> submit = () -> {
            ready.countDown();
            if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent submissions");
            }
            return mvc.perform(post("/jobs/{id}/applications", jobId).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn().getResponse().getStatus();
        };
        try {
            var first = executor.submit(submit);
            var second = executor.submit(submit);
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                second.get(15, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(201, 409);
            assertThat(applications.count()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void databaseEnforcesUniqueJobEmailConstraint() throws Exception {
        long jobId = createJob(createCompany(), "Java", "HCM", "OPEN", 10);
        mvc.perform(post("/jobs/{id}/applications", jobId).contentType(MediaType.APPLICATION_JSON)
            .content(applicationBody("an@example.com"))).andExpect(status().isCreated());
        var existing = applications.findAll().get(0);
        var duplicate = new com.example.demo.application.JobApplication(
            jobs.findById(jobId).orElseThrow(), "Another name", existing.getEmail(), "https://example.com/other.pdf");
        assertThatThrownBy(() -> applications.saveAndFlush(duplicate))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void rejectsInvalidBodiesAndReturnsConsistentErrors() throws Exception {
        mvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.path").value("/companies"));
        long companyId = createCompany();
        mvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON)
            .content(jobBody(companyId, "Java", "HCM", "OPEN", 200)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        long jobId = createJob(companyId, "Java", "HCM", "OPEN", 10);
        mvc.perform(post("/jobs/{id}/applications", jobId).contentType(MediaType.APPLICATION_JSON)
            .content(applicationBody("bad-email"))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
        mvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON).content("{broken"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"ACME\",\"address\":\"HCM\",\"website\":\"invalid\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.website").exists());
    }

    @Test
    void rejectsInvalidSearchParametersAndMissingResources() throws Exception {
        for (String query : new String[] {"page=-1", "size=0", "size=101", "sort=unknown,asc", "sort=id,sideways", "status=INVALID", "page=abc"}) {
            mvc.perform(get("/jobs?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.fieldErrors").isMap());
        }
        mvc.perform(get("/jobs").param("keyword", "x".repeat(201))).andExpect(status().isBadRequest());
        mvc.perform(get("/companies/no-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/does-not-exist")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(post("/jobs/999999/applications").contentType(MediaType.APPLICATION_JSON)
            .content(applicationBody("an@example.com"))).andExpect(status().isNotFound());
    }

    private long createCompany() throws Exception {
        String response = mvc.perform(post("/companies").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("name", "ACME", "address", "HCM", "website", "https://example.com"))))
            .andExpect(status().isCreated()).andExpect(header().string("Location", startsWith("/companies/")))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    private long createJob(long companyId, String title, String location, String status, int salary) throws Exception {
        String response = mvc.perform(post("/jobs").contentType(MediaType.APPLICATION_JSON)
            .content(jobBody(companyId, title, location, status, salary))).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }
    private String jobBody(long companyId, String title, String location, String status, int salary) throws Exception {
        return json.writeValueAsString(Map.of("companyId", companyId, "title", title, "description", "Backend development",
            "salaryFrom", salary, "salaryTo", 100, "location", location, "status", status));
    }
    private String applicationBody(String email) throws Exception {
        return json.writeValueAsString(Map.of("fullName", "Nguyễn Văn An", "email", email, "cvUrl", "https://example.com/cv.pdf"));
    }
}
