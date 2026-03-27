package com.nishant.orderservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private UUID orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
