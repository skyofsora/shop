package com.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
public class PaymentPending extends BaseEntity {
    @Id
    @GeneratedValue
    @Column(name = "payment_pending_id")
    private Long id;
    private String tid;
    private String cid;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    private Long totalPrice;

    public PaymentPending(String tid, String cid, Order order, Member member, Long totalPrice) {
        this.tid = tid;
        this.cid = cid;
        this.order = order;
        this.member = member;
        this.totalPrice = totalPrice;
    }

    public PaymentPending() {
    }
}
