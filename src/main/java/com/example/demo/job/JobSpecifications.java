package com.example.demo.job;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {
    private JobSpecifications() { }
    public static Specification<Job> filter(String keyword, String location, JobStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = pattern(keyword);
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
                    cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), pattern(location), '\\'));
            }
            if (status != null) { predicates.add(cb.equal(root.get("status"), status)); }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
    private static String pattern(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
