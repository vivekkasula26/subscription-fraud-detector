package com.fraud.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraud.subscription.event.SubscriptionEvent;
import com.fraud.subscription.event.TrialStartedEvent;
import com.fraud.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:subs;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SubscriptionApiTest {

    private static final String TRIAL_BODY =
            "{\"cardNumber\":\"4111111111111111\",\"expiry\":\"09/29\",\"cvv\":\"123\"}";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SubscriptionRepository repository;
    @MockBean KafkaTemplate<String, Object> kafka;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private String startTrial(String accountId) throws Exception {
        String body = mvc.perform(post("/api/subscriptions/trial")
                        .header("X-Account-Id", accountId)
                        .header("X-Device-Fingerprint", "dfp_2b90c7")
                        .header("X-Client-Ip", "203.0.113.42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRIAL_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").value(org.hamcrest.Matchers.startsWith("sub_")))
                .andExpect(jsonPath("$.trialEndsAt").exists())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("subscriptionId").asText();
    }

    @Test
    void startTrialStoresFingerprintOnlyAndPublishesBothEvents() throws Exception {
        String id = startTrial("acc_7f3d2a1b");

        var saved = repository.findById(id).orElseThrow();
        assertThat(saved.getCardFingerprint()).startsWith("cfp_").doesNotContain("4111");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("trial.started"), eq("acc_7f3d2a1b"), payload.capture());
        TrialStartedEvent started = (TrialStartedEvent) payload.getValue();
        assertThat(started.subscriptionId()).isEqualTo(id);
        assertThat(started.cardFingerprint()).isEqualTo(saved.getCardFingerprint());
        assertThat(started.deviceFingerprint()).isEqualTo("dfp_2b90c7");
        assertThat(started.ip()).isEqualTo("203.0.113.42");

        ArgumentCaptor<Object> eventPayload = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq("subscription.events"), eq("acc_7f3d2a1b"), eventPayload.capture());
        assertThat(((SubscriptionEvent) eventPayload.getValue()).eventType()).isEqualTo("start");
    }

    @Test
    void secondActiveTrialIsRejected() throws Exception {
        startTrial("acc_1");
        mvc.perform(post("/api/subscriptions/trial")
                        .header("X-Account-Id", "acc_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRIAL_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidCardIsRejectedWithoutEchoingIt() throws Exception {
        mvc.perform(post("/api/subscriptions/trial")
                        .header("X-Account-Id", "acc_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"abcd\",\"expiry\":\"09/29\",\"cvv\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("abcd"))));
        verify(kafka, never()).send(any(String.class), any(String.class), any());
    }

    @Test
    void missingAccountHeaderIsUnauthorized() throws Exception {
        mvc.perform(post("/api/subscriptions/trial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TRIAL_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelTrialThenCancelAgainConflicts() throws Exception {
        String id = startTrial("acc_1");

        mvc.perform(post("/api/subscriptions/{id}/cancel", id).header("X-Account-Id", "acc_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        verify(kafka).send(eq("subscription.events"), eq("acc_1"),
                org.mockito.ArgumentMatchers.argThat(o -> o instanceof SubscriptionEvent e
                        && e.eventType().equals("cancel")));

        mvc.perform(post("/api/subscriptions/{id}/cancel", id).header("X-Account-Id", "acc_1"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOtherAccountsSubscriptionIsNotFound() throws Exception {
        String id = startTrial("acc_1");
        mvc.perform(post("/api/subscriptions/{id}/cancel", id).header("X-Account-Id", "acc_2"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/subscriptions/{id}/cancel", "sub_missing").header("X-Account-Id", "acc_1"))
                .andExpect(status().isNotFound());
    }
}
