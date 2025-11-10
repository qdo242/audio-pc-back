package com.athengaudio.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.athengaudio.backend.model.User;
import com.athengaudio.backend.service.UserService;
import com.athengaudio.backend.util.JwtUtil;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Chỉ kiểm tra khi client cố gắng KẾT NỐI
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    String email = jwtUtil.extractEmail(jwt);
                    if (email != null && jwtUtil.validateToken(jwt)) {
                        User user = userService.getUserByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                        
                        // QUAN TRỌNG: Đặt ID người dùng làm Principal Name
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user.getId(), null, null);
                        accessor.setUser(auth);
                    }
                } catch (Exception e) {
                    System.err.println("WebSocket Auth Interceptor Lỗi: " + e.getMessage());
                }
            }
        }
        return message;
    }
}