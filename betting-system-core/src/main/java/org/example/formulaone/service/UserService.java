package org.example.formulaone.service;

import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.util.Constants;
import org.example.formulaone.entity.User;
import org.example.formulaone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for user-related operations including balance management.
 */
@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Ensures user exists, creates with gift balance if not found.
     */
    @Transactional
    public User ensureUserExists(UUID userId) {
        return userRepository.findById(userId).orElseGet(() -> {
            log.info("Creating new user with gift balance: {}", userId);
            User user = new User(userId, Constants.GIFT_BALANCE);
            return userRepository.save(user);
        });
    }

    /**
     * Withdraws amount from user balance if sufficient funds available.
     * 
     * @return 1 if successful, 0 if insufficient funds
     */
    @Transactional
    public int withdrawIfSufficient(UUID userId, BigDecimal amount) {
        return userRepository.withdrawIfSufficient(userId, amount);
    }

    /**
     * Credits amount to user balance.
     */
    @Transactional
    public void creditBalance(UUID userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);
        log.info("Credited {} to user {} balance", amount, userId);
    }
}
