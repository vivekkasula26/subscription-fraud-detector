package com.fraud.caseflow;

import java.time.Instant;

public record AccountActionEvent(
        String accountId,
        CaseActionType action,
        String note,
        String actorId,
        Instant timestamp) {
}
