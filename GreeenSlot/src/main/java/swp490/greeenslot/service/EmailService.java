package swp490.greeenslot.service;

public interface EmailService {

    /**
     * @return true if email was sent, false if only logged (mail not configured)
     */
    boolean sendPasswordResetEmail(String toEmail, String resetToken);

    /**
     * Sends a 6-digit registration verification OTP code to the user's email.
     */
    boolean sendRegistrationOtpEmail(String toEmail, String otp, String fullName);
}
