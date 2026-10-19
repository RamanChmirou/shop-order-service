package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.exception.CartServiceUnavailableException;
import com.kanapa4.shop.order.model.exception.EmptyCartException;
import com.kanapa4.shop.order.model.exception.InvalidOrderDataException;
import com.kanapa4.shop.order.model.exception.InvalidOrderStateException;
import com.kanapa4.shop.order.model.exception.InvoiceAlreadyIssuedException;
import com.kanapa4.shop.order.model.exception.InvoiceServiceUnavailableException;
import com.kanapa4.shop.order.model.exception.OrderConcurrentModificationException;
import com.kanapa4.shop.order.model.exception.OrderDoesNotExistException;
import com.kanapa4.shop.order.model.exception.OrderException;
import com.kanapa4.shop.order.model.exception.PriceChangedException;
import com.kanapa4.shop.order.pub.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(OrderException e) {
        log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
        return response(statusOf(e), e.getMessage(), detailsOf(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> details.putIfAbsent(error.getField(), error.getDefaultMessage()));
        log.warn("Validation failed: {}", details);
        return response(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception e) {
        log.warn("Malformed request: {}", e.getMessage());
        return response(HttpStatus.BAD_REQUEST, "Malformed request body or parameter", Map.of());
    }

    private HttpStatus statusOf(OrderException e) {
        return switch (e) {
            case OrderDoesNotExistException ex -> HttpStatus.NOT_FOUND;
            case EmptyCartException ex -> HttpStatus.BAD_REQUEST;
            case InvalidOrderDataException ex -> HttpStatus.BAD_REQUEST;
            case PriceChangedException ex -> HttpStatus.CONFLICT;
            case InvalidOrderStateException ex -> HttpStatus.CONFLICT;
            case InvoiceAlreadyIssuedException ex -> HttpStatus.CONFLICT;
            case OrderConcurrentModificationException ex -> HttpStatus.CONFLICT;
            case CartServiceUnavailableException ex -> HttpStatus.SERVICE_UNAVAILABLE;
            case InvoiceServiceUnavailableException ex -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private Map<String, Object> detailsOf(OrderException e) {
        return switch (e) {
            case PriceChangedException ex -> Map.of(
                    "expectedTotal", ex.getExpectedTotal(),
                    "actualTotal", ex.getActualTotal());
            default -> Map.of();
        };
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, Map<String, Object> details) {
        ErrorResponse body = new ErrorResponse(status.value(), status.getReasonPhrase(), message,
                Instant.now(), details);
        return ResponseEntity.status(status).body(body);
    }
}