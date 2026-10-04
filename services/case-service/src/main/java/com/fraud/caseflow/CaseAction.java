package com.fraud.caseflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_actions")
public class CaseAction {
    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CaseActionType action;

    @Column(nullable = false, length = 128)
    private String note;

    @Column(name = "actor_id", nullable = false, length = 128)
    private String actorId;

    @Column(nullable = false)
    private Instant timestamp;

    protected CaseAction() {
    }

    public CaseAction(UUID caseId, CaseActionType action, String note, String actorId, Instant timestamp) {
        this.id = UUID.randomUUID();
        this.caseId = caseId;
        this.action = action;
        this.note = note;
        this.actorId = actorId;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CaseActionType getAction() {
        return action;
    }

    public String getNote() {
        return note;
    }

    public String getActorId() {
        return actorId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
