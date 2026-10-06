package com.fraud.caseflow;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
public class CaseQueryController {
    private final FraudCaseRepository caseRepository;

    public CaseQueryController(FraudCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    @GetMapping
    public CasePageResponse getCases(
            @RequestParam(name = "status", required = false) CaseStatus status,
            @RequestParam(name = "band", required = false) RiskBand band,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Specification<FraudCase> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (band != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("band"), band));
        }
        return CasePageResponse.from(caseRepository.findAll(specification, pageable).map(CaseResponse::from));
    }

    @GetMapping("/{id}")
    public CaseResponse getCase(@PathVariable("id") UUID id) {
        FraudCase fraudCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        return CaseResponse.from(fraudCase);
    }
}
