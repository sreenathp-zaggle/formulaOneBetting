package org.example.formulaone.repository;

import org.example.formulaone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Transactional
    @Modifying
    @Query("update User u set u.balance = u.balance - :amount where u.id = :userId and u.balance >= :amount")
    int withdrawIfSufficient(@Param("userId") UUID userId, @Param("amount") BigDecimal amount);
}
