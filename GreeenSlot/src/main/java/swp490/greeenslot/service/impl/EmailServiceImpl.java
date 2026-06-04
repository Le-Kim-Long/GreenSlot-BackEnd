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
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendResetUrl + "?token=" + resetToken;
        String subject = "GreenSlot - Password Reset";
        String body = "You requested a password reset. Use the link below (valid for a limited time):\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, please ignore this email.";

        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            logger.warn("Mail is not configured. Password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        logger.info("Password reset email sent to {}", toEmail);
    }
}
