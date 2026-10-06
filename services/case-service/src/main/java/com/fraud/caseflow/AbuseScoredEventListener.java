package com.fraud.caseflow;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AbuseScoredEventListener {
    private final CaseEventService caseEventService;

    public AbuseScoredEventListener(CaseEventService caseEventService) {
        this.caseEventService = caseEventService;
    }

    @KafkaListener(topics = "${app.kafka.topics.abuse-scored}")
    public void onAbuseScored(AbuseScoredEvent event) {
        caseEventService.handle(event);
    }
}
