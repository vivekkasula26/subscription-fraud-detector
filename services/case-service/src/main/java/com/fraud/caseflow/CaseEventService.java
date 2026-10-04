package com.fraud.caseflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CaseEventService {
    private static final Logger logger = LoggerFactory.getLogger(CaseEventService.class);
    private static final String SYSTEM_ACTOR = "system";

    private final FraudCaseRepository caseRepository;
    private final CaseActionRepository actionRepository;
    private final AccountActionPublisher actionPublisher;
    private final ObjectMapper objectMapper;

    public CaseEventService(
            FraudCaseRepository caseRepository,
            CaseActionRepository actionRepository,
            AccountActionPublisher actionPublisher,
            ObjectMapper objectMapper) {
        this.caseRepository = caseRepository;
        this.actionRepository = actionRepository;
        this.actionPublisher = actionPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handle(AbuseScoredEvent event) {
        if (event.band() == RiskBand.LOW) {
            logger.info("Ignoring LOW abuse score for account {}", event.accountId());
            return;
        }

        FraudCase fraudCase = caseRepository
                .findByAccountIdAndSubscriptionId(event.accountId(), event.subscriptionId())
                .orElseGet(() -> caseRepository.saveAndFlush(
                        new FraudCase(event, objectMapper.valueToTree(event.reasons()))));

        if (event.band() == RiskBand.MEDIUM) {
            logger.info("Created or found review case {} for account {}", fraudCase.getId(), event.accountId());
            return;
        }

        if (!actionRepository.existsByCaseIdAndAction(fraudCase.getId(), CaseActionType.FREEZE)) {
            String note = "Auto-frozen: score " + event.score() + " (HIGH)";
            Instant timestamp = Instant.now();
            actionRepository.saveAndFlush(new CaseAction(
                    fraudCase.getId(), CaseActionType.FREEZE, note, SYSTEM_ACTOR, timestamp));
            actionPublisher.publish(new AccountActionEvent(
                    event.accountId(), CaseActionType.FREEZE, note, SYSTEM_ACTOR, timestamp));
        }
    }
}
