package swp490.greeenslot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import swp490.greeenslot.service.EmailService;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${greeenslot.app.frontendResetUrl:http://localhost:3000/reset-password}")
    private String frontendResetUrl;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public boolean sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendResetUrl + "?token=" + resetToken;
        String subject = "GreenSlot - Password Reset";
        String body = "You requested a password reset. Use the link below (valid for a limited time):\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, please ignore this email.";

        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            logger.warn("Mail is not configured. Password reset token for {}: {}", toEmail, resetToken);
            logger.warn("Reset link: {}", resetLink);
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        logger.info("Password reset email sent to {}", toEmail);
        return true;
    }

    @Override
    public boolean sendRegistrationOtpEmail(String toEmail, String otp, String fullName) {
        String subject = "GreenSlot - Mã xác thực đăng ký tài khoản (OTP)";
        String recipientName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";
        String body = String.format(
                "Xin chào %s,\n\n"
                + "Cảm ơn bạn đã đăng ký tài khoản tại GreenSlot!\n\n"
                + "Mã xác thực OTP của bạn là: %s\n\n"
                + "Mã này có hiệu lực trong vòng 10 phút. Vui lòng không chia sẻ mã xác thực này cho bất kỳ ai.\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ GreenSlot",
                recipientName, otp
        );

        System.out.println("==================================================================");
        System.out.printf("[GREENSLOT OTP] Mã xác thực đăng ký cho %s: %s%n", toEmail, otp);
        System.out.println("==================================================================");

        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            logger.warn("Mail is not configured. Registration OTP for {}: {}", toEmail, otp);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Registration OTP email sent successfully to {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
