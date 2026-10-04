package com.fraud.caseflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseActionRepository extends JpaRepository<CaseAction, UUID> {
    boolean existsByCaseIdAndAction(UUID caseId, CaseActionType action);
}
