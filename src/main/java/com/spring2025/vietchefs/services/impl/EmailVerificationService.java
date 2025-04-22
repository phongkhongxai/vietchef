package com.spring2025.vietchefs.services.impl;


import com.spring2025.vietchefs.models.entity.User;
import com.spring2025.vietchefs.repositories.UserRepository;
import com.spring2025.vietchefs.utils.CodeGenerator;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EmailVerificationService {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

     @Async
    public void sendVerificationCode(User user) {
        String code = CodeGenerator.generateVerificationCode();
        user.setVerificationCode(code);
        user.setVerificationCodeExpiry(LocalDateTime.now().plusMinutes(10)); // Code valid for 10 minutes

        // Send email with the verification code
        sendEmailVerify(user.getEmail(), code);
    }


    private void sendEmailVerify(String recipientEmail, String verificationCode) {
        String subject = "Email Verification Code";
        String message = "Your verification code is: " + verificationCode;

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom("apehome8386@gmail.com");
        email.setTo(recipientEmail);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }

    @Async
    public void sendPasswordResetToken(User user) {
        String token = CodeGenerator.generateVerificationCode();
        user.setResetPasswordToken(token);
        user.setResetPasswordExpiry(LocalDateTime.now().plusMinutes(15)); // Token valid for 15 minutes
        userRepository.save(user);

        // Send email with the reset token
        sendEmailReset(user.getEmail(), token);
    }


    private void sendEmailReset(String recipientEmail, String resetToken) {
        String subject = "Password Reset Request";
        String message = "To reset your password, use the following token: " + resetToken;

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom("apehome8386@gmail.com");
        email.setTo(recipientEmail);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }


    private void sendEmail(String recipientEmail, String subject, String message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom("apehome8386@gmail.com");
        email.setTo(recipientEmail);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }
    @Async
    public void sendChefRejectionEmail(User user, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("❌ Thông báo từ chối đăng ký đầu bếp - VietChefs");
            helper.setFrom("apehome8386@gmail.com");

            String content = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<style>" +
                    "  body { font-family: Arial, sans-serif; background-color: #f9f9f9; padding: 20px; }" +
                    "  .container { background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }" +
                    "  h2 { color: #e74c3c; }" +
                    "  p { font-size: 16px; color: #333; }" +
                    "  .reason { background-color: #ffe6e6; padding: 10px; border-left: 4px solid #e74c3c; margin-top: 10px; font-style: italic; }" +
                    "  .footer { margin-top: 30px; font-size: 14px; color: #777; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<h2>Đăng ký đầu bếp bị từ chối</h2>" +
                    "<p>Xin chào <strong>" + user.getFullName() + "</strong>,</p>" +
                    "<p>Cảm ơn bạn đã đăng ký trở thành đầu bếp trên nền tảng <strong>VietChefs</strong>.</p>" +
                    "<p>Sau khi xem xét hồ sơ của bạn, chúng tôi rất tiếc phải thông báo rằng <strong>hồ sơ của bạn đã bị từ chối</strong> với lý do:</p>" +
                    "<div class='reason'>" + reason + "</div>" +
                    "<p>Nếu bạn tin rằng có sai sót hoặc muốn bổ sung hồ sơ, bạn có thể đăng ký lại hoặc liên hệ với bộ phận hỗ trợ.</p>" +
                    "<p><strong>Liên hệ hỗ trợ:</strong> apehome8386@gmail.com</p>" +
                    "<div class='footer'>Trân trọng,<br><strong>Đội ngũ VietChefs 🧑‍🍳</strong></div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setText(content, true); // `true` để gửi HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email từ chối chef.");
        }
    }
    @Async
    public void sendChefApprovalEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("✅ Đăng ký đầu bếp được chấp nhận - VietChefs");
            helper.setFrom("apehome8386@gmail.com");

            String content = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<style>" +
                    "  body { font-family: Arial, sans-serif; background-color: #f0fff5; padding: 20px; }" +
                    "  .container { background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }" +
                    "  h2 { color: #27ae60; }" +
                    "  p { font-size: 16px; color: #333; }" +
                    "  .highlight { background-color: #eaffea; padding: 10px; border-left: 4px solid #27ae60; margin-top: 10px; }" +
                    "  .footer { margin-top: 30px; font-size: 14px; color: #777; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<h2>Chúc mừng bạn đã trở thành đầu bếp của VietChefs!</h2>" +
                    "<p>Xin chào <strong>" + user.getFullName() + "</strong>,</p>" +
                    "<p>Chúng tôi rất vui thông báo rằng hồ sơ đăng ký trở thành đầu bếp của bạn <strong>đã được chấp nhận</strong>.</p>" +
                    "<div class='highlight'>" +
                    "Bạn hiện đã có thể đăng nhập và bắt đầu cung cấp các món ăn tuyệt vời đến khách hàng của VietChefs!" +
                    "</div>" +
                    "<p>Nếu có bất kỳ thắc mắc nào, đừng ngần ngại liên hệ với chúng tôi qua email <strong>support@vietchefs.com</strong>.</p>" +
                    "<div class='footer'>Chúc bạn thành công,<br><strong>Đội ngũ VietChefs 🧑‍🍳</strong></div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email chấp nhận chef.");
        }
    }


}
