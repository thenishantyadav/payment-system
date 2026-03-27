package com.nishant.orderservice.service;

import com.nishant.orderservice.dto.CreateOrderRequest;
import com.nishant.orderservice.dto.OrderCreatedEvent;
import com.nishant.orderservice.dto.OrderResponse;
import com.nishant.orderservice.exception.DuplicateOrderException;
import com.nishant.orderservice.exception.OrderNotFoundException;
import com.nishant.orderservice.kafka.OrderEventPublisher;
import com.nishant.orderservice.model.Order;
import com.nishant.orderservice.model.OrderStatus;
import com.nishant.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for userId={}, idempotencyKey={}",
                request.getUserId(), request.getIdempotencyKey());

        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingOrder.isPresent()
                && !existingOrder.get().getUserId().equals(request.getUserId())) {
            throw new DuplicateOrderException("Idempotency key already used by another user");
        }

        return orderRepository.findByUserIdAndIdempotencyKey(request.getUserId(), request.getIdempotencyKey())
                .map(existing -> {
                    log.warn("Duplicate order request detected for idempotencyKey={}",
                            request.getIdempotencyKey());
                    return OrderResponse.from(existing);
                })
                .orElseGet(() -> {
                    Order order = Order.builder()
                            .userId(request.getUserId())
                            .amount(request.getAmount())
                            .currency(request.getCurrency())
                            .description(request.getDescription())
                            .idempotencyKey(request.getIdempotencyKey())
                            .status(OrderStatus.CREATED)
                            .build();

                    Order saved = orderRepository.save(order);
                    log.info("Order created successfully orderId={}", saved.getId());

                    saved.transitionTo(OrderStatus.PAYMENT_PENDING);
                    orderRepository.save(saved);

                    eventPublisher.publishOrderCreated(OrderCreatedEvent.builder()
                            .orderId(saved.getId())
                            .userId(saved.getUserId())
                            .amount(saved.getAmount())
                            .currency(saved.getCurrency())
                            .description(saved.getDescription())
                            .idempotencyKey(saved.getIdempotencyKey())
                            .createdAt(saved.getCreatedAt())
                            .build());

                    return OrderResponse.from(saved);
                });
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markOrderPaid(UUID orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.transitionTo(OrderStatus.PAID);
        order.setPaymentId(paymentId);
        orderRepository.save(order);

        log.info("Order marked as PAID orderId={}, paymentId={}", orderId, paymentId);
    }

    @Transactional
    public void markOrderFailed(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        order.transitionTo(OrderStatus.FAILED);
        order.setFailureReason(reason);
        orderRepository.save(order);

        log.error("Order marked as FAILED orderId={}, reason={}", orderId, reason);
    }
}
