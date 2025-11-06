package com.athengaudio.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOTPEmail(String toEmail, String otpCode, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("AthengAudio - Mã OTP đặt lại mật khẩu");
        message.setText("Xin chào " + (name != null ? name : "") + ",\n\n"
                + "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản AthengAudio.\n\n"
                + "Mã OTP của bạn là: " + otpCode + "\n\n"
                + "Mã OTP này có hiệu lực trong 5 phút.\n"
                + "Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\nĐội ngũ Athengudio");

        mailSender.send(message);
    }

    public void sendPasswordResetSuccessEmail(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Athengudio - Đặt lại mật khẩu thành công");
        message.setText("Xin chào " + (name != null ? name : "") + ",\n\n"
                + "Mật khẩu tài khoản AthengAudio của bạn đã được đặt lại thành công.\n\n"
                + "Nếu bạn không thực hiện thao tác này, vui lòng liên hệ ngay với bộ phận hỗ trợ.\n\n"
                + "Trân trọng,\nĐội ngũ Athengudio");

        mailSender.send(message);
    }

    public void sendWelcomeEmail(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Chào mừng đến với Athengudio!");
        message.setText("Xin chào " + name + ",\n\n"
                + "Cảm ơn bạn đã đăng ký tài khoản tại AthengAudio!\n\n"
                + "Chúc bạn có những trải nghiệm mua sắm tuyệt vời.\n\n"
                + "Trân trọng,\nĐội ngũ Athengudio");

        mailSender.send(message);
    }
}