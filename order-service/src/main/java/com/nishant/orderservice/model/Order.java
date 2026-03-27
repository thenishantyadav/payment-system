package com.nishant.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // Idempotency key — prevents double processing
    @Column(unique = true)
    private String idempotencyKey;

    private String paymentId;
    private String failureReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── STATE MACHINE ────────────────────────────────────────
    // Valid transitions:
    // CREATED → PAYMENT_PENDING
    // PAYMENT_PENDING → PAID
    // PAYMENT_PENDING → FAILED
    // PAID → REFUNDED

    public void transitionTo(OrderStatus newStatus) {
        if (!isValidTransition(this.status, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid transition: %s → %s", this.status, newStatus)
            );
        }
        this.status = newStatus;
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case CREATED         -> to == OrderStatus.PAYMENT_PENDING;
            case PAYMENT_PENDING -> to == OrderStatus.PAID || to == OrderStatus.FAILED;
            case PAID            -> to == OrderStatus.REFUNDED;
            case FAILED, REFUNDED -> false; // terminal states
        };
    }
}
