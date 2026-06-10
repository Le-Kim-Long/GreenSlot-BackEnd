package swp490.greeenslot.service;

import swp490.greeenslot.dto.ForgotPasswordRequestDTO;
import swp490.greeenslot.dto.ForgotPasswordResponseDTO;
import swp490.greeenslot.dto.JwtResponseDTO;
import swp490.greeenslot.dto.LoginRequestDTO;
import swp490.greeenslot.dto.ResetPasswordRequestDTO;
import swp490.greeenslot.dto.SignupRequestDTO;

public interface AuthService {
    JwtResponseDTO authenticateUser(LoginRequestDTO loginRequest);
    void registerUser(SignupRequestDTO signUpRequest);
    ForgotPasswordResponseDTO forgotPassword(ForgotPasswordRequestDTO request);
    void resetPassword(ResetPasswordRequestDTO request);
}
