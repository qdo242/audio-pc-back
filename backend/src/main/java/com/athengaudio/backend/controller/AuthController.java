package com.athengaudio.backend.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.athengaudio.backend.model.User;
import com.athengaudio.backend.service.UserService;
import com.athengaudio.backend.util.JwtUtil;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.authenticate(request.getEmail(), request.getPassword());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null);

            String token = jwtUtil.generateToken(user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user,
                    "token", token,
                    "message", "Đăng nhập thành công!"));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email hoặc mật khẩu không chính xác!"));
    }

    // Thêm endpoint này vào AuthController
    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email đã được sử dụng!"));
        }

        User savedUser = userService.createAdmin(user);
        savedUser.setPassword(null);

        String token = jwtUtil.generateToken(savedUser.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", savedUser,
                "token", token,
                "message", "Đăng ký tài khoản admin thành công!"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.emailExists(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email đã được sử dụng!"));
        }

        User savedUser = userService.createUser(user);
        savedUser.setPassword(null);

        String token = jwtUtil.generateToken(savedUser.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "user", savedUser,
                "token", token,
                "message", "Đăng ký thành công!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        boolean sent = userService.sendOTP(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Nếu email tồn tại, mã OTP đã được gửi đến email của bạn!"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody VerifyOTPRequest request) {
        boolean isValid = userService.verifyOTP(request.getEmail(), request.getOtpCode());

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Xác thực OTP thành công!"));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Mã OTP không hợp lệ hoặc đã hết hạn!"));
    }

    /*
     * @PostMapping("/reset-password")
     * public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest
     * request) {
     * boolean success = userService.resetPasswordWithOTP(
     * request.getEmail(),
     * request.getOtpCode(),
     * request.getNewPassword());
     * 
     * if (success) {
     * return ResponseEntity.ok(Map.of(
     * "success", true,
     * "message", "Đặt lại mật khẩu thành công!"));
     * }
     * 
     * return ResponseEntity.badRequest().body(Map.of(
     * "success", false,
     * "message", "Không thể đặt lại mật khẩu. Vui lòng kiểm tra lại thông tin!"));
     * }
     */

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        System.out.println("=== RESET PASSWORD DEBUG ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("OTP Code: " + request.getOtpCode());
        System.out.println("New Password: " + request.getNewPassword());
        System.out.println(
                "Password Length: " + (request.getNewPassword() != null ? request.getNewPassword().length() : "null"));

        try {
            boolean success = userService.resetPasswordWithOTP(
                    request.getEmail(),
                    request.getOtpCode(),
                    request.getNewPassword());

            System.out.println("Reset password result: " + success);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Đặt lại mật khẩu thành công!"));
            } else {
                System.out.println("Reset password failed - check OTP verification or user existence");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Không thể đặt lại mật khẩu. Vui lòng kiểm tra lại thông tin!"));
            }
        } catch (Exception e) {
            System.out.println("ERROR in resetPassword: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi server: " + e.getMessage()));
        }
    }

    // Request Classes
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class ForgotPasswordRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class VerifyOTPRequest {
        private String email;
        private String otpCode;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getOtpCode() {
            return otpCode;
        }

        public void setOtpCode(String otpCode) {
            this.otpCode = otpCode;
        }
    }

    public static class ResetPasswordRequest {
        private String email;
        private String otpCode;
        private String newPassword;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getOtpCode() {
            return otpCode;
        }

        public void setOtpCode(String otpCode) {
            this.otpCode = otpCode;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}