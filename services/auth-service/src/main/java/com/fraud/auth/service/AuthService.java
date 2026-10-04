package com.fraud.auth.service;

import com.fraud.auth.dto.RegisterRequest;
import com.fraud.auth.dto.RegisterResponse;
import com.fraud.auth.exception.UserAlreadyExistsException;
import com.fraud.auth.model.Account;
import com.fraud.auth.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with email " + normalizedEmail + " already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        Account account = new Account(
                request.firstName().trim(),
                request.lastName().trim(),
                normalizedEmail,
                hashedPassword,
                request.deviceId().trim()
        );

        Account savedAccount = accountRepository.save(account);

        return new RegisterResponse(
                savedAccount.getId(),
                savedAccount.getFirstName(),
                savedAccount.getLastName(),
                savedAccount.getEmail(),
                savedAccount.getStatus(),
                savedAccount.getCreatedAt()
        );
    }
}
