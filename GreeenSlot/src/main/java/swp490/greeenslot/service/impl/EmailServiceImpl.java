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

        System.out.println("==================================================================");
        System.out.printf("[GREENSLOT OTP] Mã xác thực đăng ký cho %s: %s%n", toEmail, otp);
        System.out.println("==================================================================");

        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            logger.warn("Mail is not configured (spring.mail.username is empty). Registration OTP for {}: {}", toEmail, otp);
            logger.info("To send real emails, please configure MAIL_USERNAME and MAIL_PASSWORD (Google App Password) in environment or application.yml");
            return false;
        }

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom, "GreenSlot Support");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px; background-color: #ffffff;\">"
                    + "<div style=\"text-align: center; margin-bottom: 24px;\">"
                    + "<h1 style=\"color: #059669; margin: 0; font-size: 26px;\">🌱 GreenSlot</h1>"
                    + "<p style=\"color: #6b7280; font-size: 14px; margin-top: 4px;\">Nền tảng quản lý và thuê ô vườn thông minh</p>"
                    + "</div>"
                    + "<h2 style=\"color: #111827; font-size: 18px;\">Xin chào " + recipientName + ",</h2>"
                    + "<p style=\"color: #374151; font-size: 15px; line-height: 1.6;\">Cảm ơn bạn đã đăng ký tài khoản tại <strong>GreenSlot</strong>. Vui lòng sử dụng mã OTP dưới đây để hoàn tất kích hoạt tài khoản của bạn:</p>"
                    + "<div style=\"text-align: center; margin: 30px 0;\">"
                    + "<span style=\"display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #059669; background-color: #ecfdf5; padding: 14px 28px; border-radius: 10px; border: 2px dashed #10b981; font-family: monospace;\">" + otp + "</span>"
                    + "</div>"
                    + "<p style=\"color: #4b5563; font-size: 14px;\">Mã OTP này có hiệu lực trong vòng <strong>10 phút</strong>. Vì lý do an toàn, vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;\" />"
                    + "<p style=\"color: #9ca3af; font-size: 12px; text-align: center; margin: 0;\">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.<br>© 2026 GreenSlot. All rights reserved.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("Registration OTP HTML email sent successfully to {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to send HTML email, trying plain text fallback for {}: {}", toEmail, e.getMessage());
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(String.format("Xin chào %s,\n\nMã xác thực OTP đăng ký GreenSlot của bạn là: %s (Hết hạn sau 10 phút).\n\nTrân trọng,\nĐội ngũ GreenSlot", recipientName, otp));
                mailSender.send(message);
                logger.info("Registration OTP plain text email sent successfully to {}", toEmail);
                return true;
            } catch (Exception ex) {
                logger.error("Failed to send OTP email to {}: {}", toEmail, ex.getMessage());
                return false;
            }
        }
    }
}
