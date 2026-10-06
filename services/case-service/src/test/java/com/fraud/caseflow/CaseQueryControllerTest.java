package com.fraud.caseflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseQueryController.class)
class CaseQueryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FraudCaseRepository caseRepository;

    @Test
    void getsPagedCasesFilteredByStatusAndBand() throws Exception {
        FraudCase fraudCase = fraudCase();
        when(caseRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<FraudCase>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fraudCase), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/cases")
                        .param("status", "OPEN")
                        .param("band", "HIGH")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(fraudCase.getId().toString()))
                .andExpect(jsonPath("$.content[0].accountId").value("account-1"))
                .andExpect(jsonPath("$.content[0].band").value("HIGH"))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.size").value(20));

    }

    @Test
    void getsCaseDetailById() throws Exception {
        FraudCase fraudCase = fraudCase();
        when(caseRepository.findById(fraudCase.getId())).thenReturn(Optional.of(fraudCase));

        mockMvc.perform(get("/api/cases/{id}", fraudCase.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fraudCase.getId().toString()))
                .andExpect(jsonPath("$.subscriptionId").value("subscription-1"))
                .andExpect(jsonPath("$.riskScore").value(85))
                .andExpect(jsonPath("$.reasons[0].ruleId").value("CARD_VELOCITY_24H"));
    }

    @Test
    void returnsNotFoundForUnknownCase() throws Exception {
        UUID id = UUID.randomUUID();
        when(caseRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cases/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnknownFilterValues() throws Exception {
        mockMvc.perform(get("/api/cases").param("band", "URGENT"))
                .andExpect(status().isBadRequest());
    }

    private FraudCase fraudCase() {
        AbuseScoredEvent event = new AbuseScoredEvent(
                "account-1",
                "subscription-1",
                85,
                RiskBand.HIGH,
                List.of(new AbuseScoredEvent.Reason(
                        "CARD_VELOCITY_24H", 40, "Card seen on multiple accounts")),
                Instant.parse("2026-10-05T22:00:00Z"));
        return new FraudCase(event, objectMapper.valueToTree(event.reasons()));
    }
}
