package com.nishant.orderservice.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultEvent {
    private UUID orderId;
    private String paymentId;
    private boolean success;
    private String failureReason;
}
