package com.athengaudio.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.Notification;
import com.athengaudio.backend.model.Order;
import com.athengaudio.backend.repository.OrderRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private NotificationService notificationService;
    
    // Inject ProductService để trừ kho
    @Autowired
    private ProductService productService; 

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

   public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        
        if (savedOrder != null) {
            // --- LOGIC MỚI: TRỪ TỒN KHO KHI CÓ ĐƠN MỚI ---
            for (Order.OrderItem item : savedOrder.getItems()) {
                productService.reduceStock(item.getProductId(), item.getQuantity());
            }
            // -----------------------------------------------

            // Gửi thông báo cho Admin
            Notification notif = new Notification(
                null, 
                "Có đơn hàng mới #" + savedOrder.getId().substring(0, 6) + "!",
                "/admin" 
            );
            notificationService.notifyAdmin(notif);
        }
        return savedOrder;
    }

    public Order updateOrderStatus(String id, Order.OrderStatus status) {
        Optional<Order> orderOptional = orderRepository.findById(id);
        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            order.setStatus(status);
            Order updatedOrder = orderRepository.save(order);

            // Gửi thông báo cho User
            String message = "Đơn hàng #" + id.substring(0, 6) + " của bạn đã được " + getStatusText(status) + ".";
            Notification notif = new Notification(
                order.getUserId(), 
                message,
                "/orders/" + id 
            );
            notificationService.sendNotificationToUser(notif);

            return updatedOrder;
        }
        return null;
    }

    private String getStatusText(Order.OrderStatus status) {
        switch (status) {
            case CONFIRMED: return "xác nhận";
            case PROCESSING: return "đang xử lý";
            case SHIPPED: return "đang giao";
            case DELIVERED: return "giao thành công";
            case CANCELLED: return "hủy";
            default: return "cập nhật";
        }
    }

    public boolean deleteOrder(String id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }
}