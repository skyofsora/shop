package com.shop.repository;

import com.shop.entity.PaymentPending;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentPendingRepository extends JpaRepository<PaymentPending, Long> {
    PaymentPending findByOrderId(Long orderId);
}
