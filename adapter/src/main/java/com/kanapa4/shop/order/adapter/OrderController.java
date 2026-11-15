package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.domain.service.OrderService;
import com.kanapa4.shop.order.pub.page.Page;
import com.kanapa4.shop.order.pub.page.Pageable;
import com.kanapa4.shop.order.pub.page.SortDirection;
import com.kanapa4.shop.order.pub.request.CheckoutRequest;
import com.kanapa4.shop.order.pub.response.OrderResponse;
import com.kanapa4.shop.order.pub.response.OrderSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, payment, shipping and order history")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place an order from the user's cart (prices are re-verified)")
    @ApiResponse(responseCode = "409", description = "Cart total changed since the user saw it")
    public OrderResponse checkout(@PathVariable UUID userId, @Valid @RequestBody CheckoutRequest request) {
        log.info("POST /orders/{}", userId);
        return orderService.checkout(userId, request);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "List the user's orders (paged)")
    public Page<OrderSummaryResponse> getOrders(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") SortDirection direction) {
        log.info("GET /orders/{} page={} size={} sortBy={} direction={}", userId, page, size, sortBy, direction);
        return orderService.getOrders(userId, new Pageable(page, size, sortBy, direction));
    }

    @GetMapping("/{userId}/{orderId}")
    @Operation(summary = "Get one order of the user")
    public OrderResponse getOrder(@PathVariable UUID userId, @PathVariable UUID orderId) {
        log.info("GET /orders/{}/{}", userId, orderId);
        return orderService.getOrder(userId, orderId);
    }

    @PostMapping("/{userId}/{orderId}/pay")
    @Operation(summary = "Pay for an order and issue its invoice")
    public OrderResponse pay(@PathVariable UUID userId, @PathVariable UUID orderId) {
        log.info("POST /orders/{}/{}/pay", userId, orderId);
        return orderService.pay(userId, orderId);
    }

    @PostMapping("/{userId}/{orderId}/cancel")
    @Operation(summary = "Cancel an unpaid order")
    public OrderResponse cancel(@PathVariable UUID userId, @PathVariable UUID orderId) {
        log.info("POST /orders/{}/{}/cancel", userId, orderId);
        return orderService.cancel(userId, orderId);
    }

    @PostMapping("/{userId}/{orderId}/invoice")
    @Operation(summary = "Retry issuing the invoice of a paid order")
    public OrderResponse retryInvoice(@PathVariable UUID userId, @PathVariable UUID orderId) {
        log.info("POST /orders/{}/{}/invoice", userId, orderId);
        return orderService.retryInvoice(userId, orderId);
    }

    @PostMapping("/{orderId}/ship")
    @Operation(summary = "Mark a paid order as shipped (staff action)")
    public OrderResponse ship(@PathVariable UUID orderId) {
        log.info("POST /orders/{}/ship", orderId);
        return orderService.ship(orderId);
    }
}