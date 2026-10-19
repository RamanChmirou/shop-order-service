package com.kanapa4.shop.order.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"items", "invoice"})
    Optional<OrderEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"items", "invoice"})
    Optional<OrderEntity> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = "invoice")
    Page<OrderEntity> findAllByUserId(UUID userId, Pageable pageable);
}