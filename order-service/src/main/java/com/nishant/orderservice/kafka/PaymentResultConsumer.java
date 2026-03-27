package com.nishant.orderservice.kafka;

import com.nishant.orderservice.dto.PaymentResultEvent;
import com.nishant.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultConsumer {

    private final OrderService orderService;

    @KafkaListener(
        topics = {"${kafka.topics.payment-success}", "${kafka.topics.payment-failed}"},
        groupId = "order-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentResult(
            @Payload PaymentResultEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received PaymentResultEvent: orderId={}, success={}, topic={}, partition={}, offset={}",
            event.getOrderId(), event.isSuccess(), topic, partition, offset);

        try {
            if (event.isSuccess()) {
                orderService.markOrderPaid(event.getOrderId(), event.getPaymentId());
            } else {
                orderService.markOrderFailed(event.getOrderId(), event.getFailureReason());
            }
        } catch (Exception e) {
            log.error("Error processing PaymentResultEvent for orderId={}: {}",
                event.getOrderId(), e.getMessage());
            // In production: send to Dead Letter Topic
            throw e;
        }
    }
}
