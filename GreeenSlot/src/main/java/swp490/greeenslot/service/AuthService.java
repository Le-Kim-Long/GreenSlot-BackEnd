package swp490.greeenslot.service;

import swp490.greeenslot.dto.JwtResponseDTO;
import swp490.greeenslot.dto.LoginRequestDTO;
import swp490.greeenslot.dto.SignupRequestDTO;

public interface AuthService {
    JwtResponseDTO authenticateUser(LoginRequestDTO loginRequest);
    void registerUser(SignupRequestDTO signUpRequest);
}
