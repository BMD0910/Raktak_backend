package com.raktakk.backend.service;

import com.raktakk.backend.entity.*;
import com.raktakk.backend.dto.OrderDTO;
import com.raktakk.backend.dto.CreateOrderRequest;
import com.raktakk.backend.repository.OrderRepository;
import com.raktakk.backend.repository.ServiceOfferRepository;
import com.raktakk.backend.repository.UserRepository;
import com.raktakk.backend.exception.BadRequestException;
import com.raktakk.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ServiceOfferRepository serviceOfferRepository;
    private final UserRepository userRepository;
    private final ConversationServiceImpl conversationService;

    @Transactional
    public OrderDTO createOrder(String clientEmail, CreateOrderRequest request) {
        User client = userRepository.findByEmailIgnoreCase(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ServiceOffer service = serviceOfferRepository.findById(request.serviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        if (service.getVendorProfile().getUser().getId().equals(client.getId())) {
            throw new BadRequestException("Vous ne pouvez pas commander votre propre service");
        }

        Order order = Order.builder()
                .client(client)
                .vendor(service.getVendorProfile().getUser())
                .service(service)
                .description(request.description())
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);
        return toOrderDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getClientOrders(String clientEmail) {
        User client = userRepository.findByEmailIgnoreCase(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        return orderRepository.findByClientIdOrderByCreatedAtDesc(client.getId())
                .stream()
                .map(this::toOrderDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getVendorOrders(String vendorEmail) {
        User vendor = userRepository.findByEmailIgnoreCase(vendorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        return orderRepository.findByVendorIdOrderByCreatedAtDesc(vendor.getId())
                .stream()
                .map(this::toOrderDTO)
                .toList();
    }

    @Transactional
    public OrderDTO acceptOrder(String vendorEmail, Long orderId) {
        User vendor = userRepository.findByEmailIgnoreCase(vendorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        Order order = orderRepository.findByIdAndVendorId(orderId, vendor.getId())
                .orElseThrow(() -> new BadRequestException("Commande introuvable ou non autorisée"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Seules les demandes en attente peuvent être acceptées");
        }

        order.setStatus(OrderStatus.ACCEPTED);
        Order saved = orderRepository.save(order);

        // Créer automatiquement une conversation
        conversationService.createConversation(saved);

        return toOrderDTO(saved);
    }

    @Transactional
    public OrderDTO rejectOrder(String vendorEmail, Long orderId) {
        User vendor = userRepository.findByEmailIgnoreCase(vendorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        Order order = orderRepository.findByIdAndVendorId(orderId, vendor.getId())
                .orElseThrow(() -> new BadRequestException("Commande introuvable ou non autorisée"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Seules les demandes en attente peuvent être refusées");
        }

        order.setStatus(OrderStatus.REJECTED);
        return toOrderDTO(orderRepository.save(order));
    }

    @Transactional
    public OrderDTO cancelOrder(String clientEmail, Long orderId) {
        User client = userRepository.findByEmailIgnoreCase(clientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Order order = orderRepository.findByIdAndClientId(orderId, client.getId())
                .orElseThrow(() -> new BadRequestException("Commande introuvable ou non autorisée"));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible d'annuler une commande terminée ou déjà annulée");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toOrderDTO(orderRepository.save(order));
    }

    @Transactional
    public OrderDTO completeOrder(String vendorEmail, Long orderId) {
        User vendor = userRepository.findByEmailIgnoreCase(vendorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));

        Order order = orderRepository.findByIdAndVendorId(orderId, vendor.getId())
                .orElseThrow(() -> new BadRequestException("Commande introuvable ou non autorisée"));

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new BadRequestException("Seules les commandes acceptées peuvent être complétées");
        }

        order.setStatus(OrderStatus.COMPLETED);
        return toOrderDTO(orderRepository.save(order));
    }

    private OrderDTO toOrderDTO(Order order) {
        return new OrderDTO(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getFullName(),
                order.getVendor().getId(),
                order.getVendor().getFullName(),
                order.getService().getId(),
                order.getService().getTitle(),
                order.getService().getPrice(),
                order.getStatus(),
                order.getDescription(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
