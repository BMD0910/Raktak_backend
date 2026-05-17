package com.raktakk.backend.controller;

import com.raktakk.backend.dto.OrderDTO;
import com.raktakk.backend.dto.CreateOrderRequest;
import com.raktakk.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Create a new order (client creates request for vendor's service)
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        OrderDTO orderDTO = orderService.createOrder(userEmail, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderDTO);
    }

    /**
     * Get all orders for authenticated client
     */
    @GetMapping("/client")
    public ResponseEntity<List<OrderDTO>> getClientOrders(Authentication authentication) {
        String userEmail = authentication.getName();
        List<OrderDTO> orders = orderService.getClientOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get all orders for authenticated vendor
     */
    @GetMapping("/vendor")
    public ResponseEntity<List<OrderDTO>> getVendorOrders(Authentication authentication) {
        String userEmail = authentication.getName();
        List<OrderDTO> orders = orderService.getVendorOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * Accept an order (vendor accepts client's request)
     */
    @PostMapping("/{orderId}/accept")
    public ResponseEntity<OrderDTO> acceptOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        OrderDTO orderDTO = orderService.acceptOrder(userEmail, orderId);
        return ResponseEntity.ok(orderDTO);
    }

    /**
     * Reject an order (vendor declines client's request)
     */
    @PostMapping("/{orderId}/reject")
    public ResponseEntity<OrderDTO> rejectOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        OrderDTO orderDTO = orderService.rejectOrder(userEmail, orderId);
        return ResponseEntity.ok(orderDTO);
    }

    /**
     * Cancel an order (client cancels order)
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        OrderDTO orderDTO = orderService.cancelOrder(userEmail, orderId);
        return ResponseEntity.ok(orderDTO);
    }

    /**
     * Complete an order (vendor marks service as completed)
     */
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<OrderDTO> completeOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        OrderDTO orderDTO = orderService.completeOrder(userEmail, orderId);
        return ResponseEntity.ok(orderDTO);
    }
}
