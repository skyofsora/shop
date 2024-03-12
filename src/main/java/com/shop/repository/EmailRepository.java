package com.shop.repository;

import com.shop.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface EmailRepository extends JpaRepository<Email, Long> {
    Email findByEmail(String email);
    @Transactional
    void deleteByEmail(String email);
}
