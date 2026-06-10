package swp490.greeenslot.service;

public interface EmailService {

    /**
     * @return true if email was sent, false if only logged (mail not configured)
     */
    boolean sendPasswordResetEmail(String toEmail, String resetToken);
}
