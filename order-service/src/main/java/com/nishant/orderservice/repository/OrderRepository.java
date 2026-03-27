package com.nishant.orderservice.repository;

import com.nishant.orderservice.model.Order;
import com.nishant.orderservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByIdAndUserId(UUID id, String userId);
}
