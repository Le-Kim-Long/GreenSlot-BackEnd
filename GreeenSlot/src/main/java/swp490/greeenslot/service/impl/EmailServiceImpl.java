package swp490.greeenslot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:caocongtanthong@gmail.com}")
    private String brevoSenderEmail;

    @Value("${brevo.sender-name:GreenSlot Support}")
    private String brevoSenderName;

    private String resolveBrevoApiKey() {
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            return brevoApiKey.trim();
        }
        String envKey = System.getenv("BREVO_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        String propKey = System.getProperty("BREVO_API_KEY");
        if (propKey != null && !propKey.isBlank()) {
            return propKey.trim();
        }
        return null;
    }

    private boolean sendViaBrevoHttpApi(String toEmail, String recipientName, String subject, String htmlContent) {
        String apiKey = resolveBrevoApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> payload = new java.util.HashMap<>();

            java.util.Map<String, String> sender = new java.util.HashMap<>();
            sender.put("name", brevoSenderName != null ? brevoSenderName : "GreenSlot Support");
            sender.put("email", (brevoSenderEmail != null && !brevoSenderEmail.isBlank()) ? brevoSenderEmail : "caocongtanthong@gmail.com");
            payload.put("sender", sender);

            java.util.List<java.util.Map<String, String>> toList = new java.util.ArrayList<>();
            java.util.Map<String, String> to = new java.util.HashMap<>();
            to.put("email", toEmail);
            to.put("name", recipientName);
            toList.add(to);
            payload.put("to", toList);

            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String requestBody = mapper.writeValueAsString(payload);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", apiKey)
                    .header("content-type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Email sent successfully via Brevo HTTP API to {}. Response: {}", toEmail, response.body());
                return true;
            } else {
                logger.warn("Brevo HTTP API returned status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception sending email via Brevo HTTP API to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendResetUrl + "?token=" + resetToken;
        String subject = "GreenSlot - Password Reset";
        String body = "You requested a password reset. Use the link below (valid for a limited time):\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, please ignore this email.";

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;\">"
                + "<h2>GreenSlot - Đặt lại mật khẩu</h2>"
                + "<p>Bạn đã yêu cầu đặt lại mật khẩu. Bấm vào liên kết bên dưới để hoàn tất:</p>"
                + "<p><a href=\"" + resetLink + "\" style=\"display: inline-block; padding: 10px 20px; background-color: #059669; color: white; text-decoration: none; border-radius: 6px;\">Đặt lại mật khẩu</a></p>"
                + "<p style=\"color: #6b7280; font-size: 13px;\">Nếu không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>"
                + "</div>";

        if (sendViaBrevoHttpApi(toEmail, "Quý khách", subject, htmlContent)) {
            return true;
        }

        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            logger.warn("Mail is not configured. Password reset token for {}: {}", toEmail, resetToken);
            logger.warn("Reset link: {}", resetLink);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("Password reset email sent to {}", toEmail);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    @Async
    @Override
    public void sendRegistrationOtpEmail(String toEmail, String otp, String fullName) {
        String subject = "GreenSlot - Mã xác thực đăng ký tài khoản (OTP)";
        String recipientName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";

        System.out.println("==================================================================");
        System.out.printf("[GREENSLOT OTP] Mã xác thực đăng ký cho %s: %s%n", toEmail, otp);
        System.out.println("==================================================================");

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

        // 1. Ưu tiên gửi qua Brevo HTTP API (Cổng HTTPS 443 - không bị Cloud Render chặn)
        if (resolveBrevoApiKey() != null) {
            boolean sentViaBrevo = sendViaBrevoHttpApi(toEmail, recipientName, subject, htmlContent);
            if (sentViaBrevo) {
                logger.info("Registration OTP email delivered via Brevo HTTP API successfully to {}", toEmail);
                return;
            }
            logger.warn("Brevo HTTP API delivery failed, attempting SMTP fallback...");
        }

        // 2. Fallback sang JavaMailSender SMTP (chạy trên local)
        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            logger.warn("Mail is not configured (spring.mail.username is empty). Registration OTP for {}: {}", toEmail, otp);
            logger.info("To send real emails, please configure BREVO_API_KEY or MAIL_USERNAME/MAIL_PASSWORD in environment");
            return;
        }

        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom, "GreenSlot Support");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            logger.info("Registration OTP HTML email sent successfully via SMTP to {}", toEmail);
        } catch (Exception e) {
            logger.warn("Failed to send HTML email via SMTP, trying plain text fallback for {}: {}", toEmail, e.getMessage());
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(String.format("Xin chào %s,\n\nMã xác thực OTP đăng ký GreenSlot của bạn là: %s (Hết hạn sau 10 phút).\n\nTrân trọng,\nĐội ngũ GreenSlot", recipientName, otp));
                mailSender.send(message);
                logger.info("Registration OTP plain text email sent successfully via SMTP to {}", toEmail);
            } catch (Exception ex) {
                logger.error("Failed to send OTP email to {} (Cloud hosts like Render block SMTP ports 25/465/587. Configure BREVO_API_KEY for 100% cloud email delivery): {}", toEmail, ex.getMessage());
            }
        }
    }
}
