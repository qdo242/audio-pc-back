package com.athengaudio.backend.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.athengaudio.backend.model.User;
import com.athengaudio.backend.repository.UserRepository;
import com.athengaudio.backend.util.JwtUtil;
import com.athengaudio.backend.util.PasswordEncoderUtil;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoderUtil passwordEncoder;

    @Autowired
    @SuppressWarnings("unused")
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OTPService otpService;

    // OTP Methods
    public boolean sendOTP(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            String otp = otpService.generateOTP();
            Date expiryDate = otpService.calculateExpiryDate();

            // ĐẢM BẢO SET CÁC TRƯỜNG OTP
            user.setOtpCode(otp);
            user.setOtpExpiry(expiryDate);
            user.setOtpAttempts(0);
            user.setUpdatedAt(new Date());

            @SuppressWarnings("unused")
            User savedUser = userRepository.save(user); // QUAN TRỌNG: phải save lại

            System.out.println("✅ OTP saved for user: " + email);
            System.out.println("OTP: " + otp);
            System.out.println("Expiry: " + expiryDate);

            emailService.sendOTPEmail(email, otp, user.getName());
            return true;
        }
        return false;
    }

    public boolean verifyOTP(String email, String otpCode) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Kiểm tra null values trước khi validate
            if (user.getOtpCode() == null || user.getOtpExpiry() == null) {
                return false;
            }

            boolean isValid = otpService.validateOTP(
                    otpCode,
                    user.getOtpCode(),
                    user.getOtpExpiry(),
                    user.getOtpAttempts());

            if (isValid) {
                user.setOtpCode(null);
                user.setOtpExpiry(null);
                user.setOtpAttempts(null);
                user.setUpdatedAt(new Date());
                userRepository.save(user);
                return true;
            } else {
                Integer attempts = user.getOtpAttempts() != null ? user.getOtpAttempts() + 1 : 1;
                user.setOtpAttempts(attempts);
                user.setUpdatedAt(new Date());
                userRepository.save(user);
                return false;
            }
        }
        return false;
    }

    public boolean resetPasswordWithOTP(String email, String otpCode, String newPassword) {
        // Verify OTP first
        if (!verifyOTP(email, otpCode)) {
            return false;
        }

        // Then reset password
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(new Date());
            userRepository.save(user);

            emailService.sendPasswordResetSuccessEmail(email, user.getName());
            return true;
        }
        return false;
    }

    // Existing Methods
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User createUser(User user) {
        user.setRole("user");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        if (user.getWishlist() == null) {
            user.setWishlist(List.of());
        }

        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());
        return savedUser;
    }

    public User createAdmin(User user) {
        user.setRole("admin");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        if (user.getWishlist() == null) {
            user.setWishlist(List.of());
        }
        return userRepository.save(user);
    }

    public Optional<User> authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }

    public User updateUser(String id, User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    if (userDetails.getName() != null)
                        user.setName(userDetails.getName());
                    if (userDetails.getPhone() != null)
                        user.setPhone(userDetails.getPhone());
                    if (userDetails.getAddress() != null)
                        user.setAddress(userDetails.getAddress());
                    if (userDetails.getAvatar() != null)
                        user.setAvatar(userDetails.getAvatar());
                    user.setUpdatedAt(new Date());
                    return userRepository.save(user);
                })
                .orElse(null);
    }

    public boolean deleteUser(String id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}