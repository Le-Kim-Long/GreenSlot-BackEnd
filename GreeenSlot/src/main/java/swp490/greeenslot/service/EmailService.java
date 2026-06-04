package swp490.greeenslot.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetToken);
}
