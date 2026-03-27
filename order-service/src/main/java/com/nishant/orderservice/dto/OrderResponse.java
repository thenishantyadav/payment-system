package com.nishant.orderservice.dto;

import com.nishant.orderservice.model.Order;
import com.nishant.orderservice.model.OrderStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private UUID id;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private OrderStatus status;
    private String idempotencyKey;
    private String paymentId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .description(order.getDescription())
                .status(order.getStatus())
                .idempotencyKey(order.getIdempotencyKey())
                .paymentId(order.getPaymentId())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
