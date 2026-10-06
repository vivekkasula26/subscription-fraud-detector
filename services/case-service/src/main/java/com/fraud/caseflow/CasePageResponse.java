package com.fraud.caseflow;

import org.springframework.data.domain.Page;

import java.util.List;

public record CasePageResponse(List<CaseResponse> content, PageMetadata page) {
    public CasePageResponse {
        content = List.copyOf(content);
    }

    public static CasePageResponse from(Page<CaseResponse> cases) {
        return new CasePageResponse(
                cases.getContent(),
                new PageMetadata(
                        cases.getNumber(),
                        cases.getSize(),
                        cases.getTotalElements(),
                        cases.getTotalPages()));
    }

    public record PageMetadata(int number, int size, long totalElements, int totalPages) {
    }
}
