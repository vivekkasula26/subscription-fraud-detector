package com.fraud.subscription.web;

import com.fraud.subscription.dto.CancelResponse;
import com.fraud.subscription.dto.StartTrialRequest;
import com.fraud.subscription.dto.StartTrialResponse;
import com.fraud.subscription.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;
    private final AccountContextResolver accountResolver;

    public SubscriptionController(SubscriptionService service, AccountContextResolver accountResolver) {
        this.service = service;
        this.accountResolver = accountResolver;
    }

    @PostMapping("/trial")
    @ResponseStatus(HttpStatus.CREATED)
    public StartTrialResponse startTrial(@Valid @RequestBody StartTrialRequest request,
                                         HttpServletRequest http) {
        return service.startTrial(accountResolver.resolve(http), request);
    }

    @PostMapping("/{id}/cancel")
    public CancelResponse cancel(@PathVariable("id") String id, HttpServletRequest http) {
        return service.cancel(accountResolver.resolve(http), id);
    }
}
