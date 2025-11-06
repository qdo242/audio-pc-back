package com.athengaudio.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.Cart;
import com.athengaudio.backend.repository.CartRepository;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    public Optional<Cart> getCartByUserId(String userId) {
        return cartRepository.findByUserId(userId);
    }

    public Cart createOrUpdateCart(Cart cart) {
        return cartRepository.save(cart);
    }

    public Cart addItemToCart(String userId, Cart.CartItem item) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElse(new Cart(userId));

        cart.addItem(item);
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(String userId, String productId, Integer quantity) {
        Optional<Cart> cartOptional = cartRepository.findByUserId(userId);
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            cart.updateQuantity(productId, quantity);
            return cartRepository.save(cart);
        }
        return null;
    }

    public Cart removeItemFromCart(String userId, String productId) {
        Optional<Cart> cartOptional = cartRepository.findByUserId(userId);
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            cart.removeItem(productId);
            return cartRepository.save(cart);
        }
        return null;
    }

    public Cart clearCart(String userId) {
        Optional<Cart> cartOptional = cartRepository.findByUserId(userId);
        if (cartOptional.isPresent()) {
            Cart cart = cartOptional.get();
            cart.clearCart();
            return cartRepository.save(cart);
        }
        return null;
    }

    public void deleteCart(String userId) {
        cartRepository.deleteByUserId(userId);
    }
}