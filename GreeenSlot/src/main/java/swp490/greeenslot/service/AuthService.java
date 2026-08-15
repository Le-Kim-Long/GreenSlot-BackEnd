package swp490.greeenslot.service;

import swp490.greeenslot.dto.ForgotPasswordRequestDTO;
import swp490.greeenslot.dto.ForgotPasswordResponseDTO;
import swp490.greeenslot.dto.GoogleLoginRequestDTO;
import swp490.greeenslot.dto.JwtResponseDTO;
import swp490.greeenslot.dto.LoginRequestDTO;
import swp490.greeenslot.dto.ResendOtpRequestDTO;
import swp490.greeenslot.dto.ResetPasswordRequestDTO;
import swp490.greeenslot.dto.SignupRequestDTO;
import swp490.greeenslot.dto.VerifyOtpRequestDTO;

public interface AuthService {
    JwtResponseDTO authenticateUser(LoginRequestDTO loginRequest);
    JwtResponseDTO authenticateWithGoogle(GoogleLoginRequestDTO googleRequest);
    void registerUser(SignupRequestDTO signUpRequest);
    JwtResponseDTO verifyRegistrationOtp(VerifyOtpRequestDTO request);
    void resendRegistrationOtp(String email);
    ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO request);
    void resetPassword(ResetPasswordRequestDTO request);
}
