package com.nishant.orderservice.controller;

import com.nishant.orderservice.dto.CreateOrderRequest;
import com.nishant.orderservice.dto.OrderResponse;
import com.nishant.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("POST /api/v1/orders userId={}", userDetails.getUsername());
        request.setUserId(userDetails.getUsername());
        request.setIdempotencyKey(idempotencyKey);

        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/orders/{} userId={}", orderId, userDetails.getUsername());
        OrderResponse response = orderService.getOrder(orderId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("GET /api/v1/orders (all) userId={}", userDetails.getUsername());
        List<OrderResponse> orders = orderService.getUserOrders(userDetails.getUsername());
        return ResponseEntity.ok(orders);
    }
}
