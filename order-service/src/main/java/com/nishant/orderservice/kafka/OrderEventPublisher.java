package com.nishant.orderservice.kafka;

import com.nishant.orderservice.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId={}", event.getOrderId());

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
                kafkaTemplate.send(orderCreatedTopic, event.getOrderId().toString(), event);

        try {
            SendResult<String, OrderCreatedEvent> result = future.get(10, TimeUnit.SECONDS);
            log.info("OrderCreatedEvent published successfully orderId={}, partition={}, offset={}",
                    event.getOrderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing order event", e);
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to publish OrderCreatedEvent for orderId={}, error={}",
                    event.getOrderId(), e.getMessage());
            throw new IllegalStateException("Failed to publish order event", e);
        }
    }
}
