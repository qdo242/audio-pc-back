package com.athengaudio.backend.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "carts")
public class Cart {
    @Id
    private String id;

    private String userId; // Hoặc @DBRef nếu muốn reference

    private List<CartItem> items = new ArrayList<>();
    private Double totalAmount = 0.0;

    // Constructors
    public Cart() {
    }

    public Cart(String userId) {
        this.userId = userId;
    }

    // Inner class for CartItem
    public static class CartItem {
        private String productId;
        private String productName;
        private Double price;
        private Integer quantity;
        private Double subTotal;

        // Constructors, Getters, Setters
        public CartItem() {
        }

        public CartItem(String productId, String productName, Double price, Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
            this.subTotal = price * quantity;
        }

        // Getters and Setters
        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
            if (this.price != null) {
                this.subTotal = this.price * quantity;
            }
        }

        public Double getSubTotal() {
            return subTotal;
        }

        public void setSubTotal(Double subTotal) {
            this.subTotal = subTotal;
        }
    }

    // Methods to calculate total
    public void calculateTotal() {
        this.totalAmount = items.stream()
                .mapToDouble(CartItem::getSubTotal)
                .sum();
    }

    public void addItem(CartItem item) {
        // Check if item already exists
        for (CartItem existingItem : items) {
            if (existingItem.getProductId().equals(item.getProductId())) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                calculateTotal();
                return;
            }
        }
        items.add(item);
        calculateTotal();
    }

    public void removeItem(String productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
        calculateTotal();
    }

    public void updateQuantity(String productId, Integer quantity) {
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(quantity);
                calculateTotal();
                return;
            }
        }
    }

    public void clearCart() {
        items.clear();
        totalAmount = 0.0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        calculateTotal();
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}