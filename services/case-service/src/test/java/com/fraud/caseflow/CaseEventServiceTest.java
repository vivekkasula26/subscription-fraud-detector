package com.fraud.caseflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseEventServiceTest {
    private FraudCaseRepository caseRepository;
    private CaseActionRepository actionRepository;
    private AccountActionPublisher actionPublisher;
    private CaseEventService caseEventService;

    @BeforeEach
    void setUp() {
        caseRepository = mock(FraudCaseRepository.class);
        actionRepository = mock(CaseActionRepository.class);
        actionPublisher = mock(AccountActionPublisher.class);
        caseEventService = new CaseEventService(
                caseRepository, actionRepository, actionPublisher, new ObjectMapper());
    }

    @Test
    void lowScoreDoesNotCreateCaseOrPublishAction() {
        caseEventService.handle(event(20, RiskBand.LOW));

        verify(caseRepository, never()).saveAndFlush(any());
        verify(actionRepository, never()).saveAndFlush(any());
        verify(actionPublisher, never()).publish(any());
    }

    @Test
    void mediumScoreCreatesOpenCaseWithoutPublishingAction() {
        when(caseRepository.findByAccountIdAndSubscriptionId("account-1", "subscription-1"))
                .thenReturn(Optional.empty());
        when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        caseEventService.handle(event(55, RiskBand.MEDIUM));

        ArgumentCaptor<FraudCase> caseCaptor = ArgumentCaptor.forClass(FraudCase.class);
        verify(caseRepository).saveAndFlush(caseCaptor.capture());
        FraudCase savedCase = caseCaptor.getValue();
        assertEquals("account-1", savedCase.getAccountId());
        assertEquals("subscription-1", savedCase.getSubscriptionId());
        assertEquals(55, savedCase.getRiskScore());
        assertEquals(RiskBand.MEDIUM, savedCase.getBand());
        assertEquals(CaseStatus.OPEN, savedCase.getStatus());
        assertEquals(1, savedCase.getReasons().size());
        verify(actionPublisher, never()).publish(any());
    }

    @Test
    void highScoreCreatesCaseAndAuditsFreezeBeforePublishing() {
        when(caseRepository.findByAccountIdAndSubscriptionId("account-1", "subscription-1"))
                .thenReturn(Optional.empty());
        when(caseRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.existsByCaseIdAndAction(any(), any())).thenReturn(false);
        when(actionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        caseEventService.handle(event(85, RiskBand.HIGH));

        ArgumentCaptor<CaseAction> actionCaptor = ArgumentCaptor.forClass(CaseAction.class);
        verify(actionRepository).saveAndFlush(actionCaptor.capture());
        CaseAction action = actionCaptor.getValue();
        assertEquals(CaseActionType.FREEZE, action.getAction());
        assertEquals("system", action.getActorId());
        assertNotNull(action.getCaseId());

        ArgumentCaptor<AccountActionEvent> eventCaptor = ArgumentCaptor.forClass(AccountActionEvent.class);
        verify(actionPublisher).publish(eventCaptor.capture());
        AccountActionEvent published = eventCaptor.getValue();
        assertEquals("account-1", published.accountId());
        assertEquals(CaseActionType.FREEZE, published.action());
        assertEquals(action.getNote(), published.note());
        assertEquals(action.getTimestamp(), published.timestamp());
    }

    @Test
    void duplicateHighEventDoesNotCreateAnotherCaseOrFreezeAudit() {
        FraudCase existingCase = new FraudCase(event(85, RiskBand.HIGH), new ObjectMapper().valueToTree(List.of()));
        when(caseRepository.findByAccountIdAndSubscriptionId("account-1", "subscription-1"))
                .thenReturn(Optional.of(existingCase));
        when(actionRepository.existsByCaseIdAndAction(existingCase.getId(), CaseActionType.FREEZE))
                .thenReturn(true);

        caseEventService.handle(event(85, RiskBand.HIGH));

        verify(caseRepository, never()).saveAndFlush(any());
        verify(actionRepository, never()).saveAndFlush(any());
        verify(actionPublisher, never()).publish(any());
    }

    private AbuseScoredEvent event(int score, RiskBand band) {
        return new AbuseScoredEvent(
                "account-1",
                "subscription-1",
                score,
                band,
                List.of(new AbuseScoredEvent.Reason("DEVICE_REUSE", 35, "Device seen on two accounts")),
                Instant.parse("2026-10-03T20:00:00Z"));
    }
}
