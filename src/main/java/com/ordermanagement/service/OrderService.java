package com.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.ordermanagement.entity.Order;
import com.ordermanagement.repository.OrderRepository;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public Order placeOrder(Order order) {
        order.setStatus("PENDING");
        Order savedOrder = orderRepository.save(order);

        // Publish order placed event to Kafka
        kafkaTemplate.send("order_topic", "ORDER_PLACED: " + savedOrder.getId());

        return savedOrder;
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public String cancelOrder(Long orderId) {
        Optional<Order> optionalOrder = orderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            if ("PENDING".equals(order.getStatus())) {
                order.setStatus("CANCELED");
                orderRepository.save(order);

                // Publish order canceled event
                kafkaTemplate.send("order_topic", "ORDER_CANCELED: " + order.getId());

                return "Order Canceled Successfully";
            } else {
                return "Cannot cancel order, status: " + order.getStatus();
            }
        }
        return "Order Not Found";
    }
}
