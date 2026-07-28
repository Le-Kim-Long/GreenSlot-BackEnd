package swp490.greeenslot.service;

import swp490.greeenslot.dto.ProfileResponseDTO;
import swp490.greeenslot.dto.UserProfileUpdateDTO;
import swp490.greeenslot.entity.User;

public interface UserService {
    ProfileResponseDTO getProfile(String username);
    java.util.List<ProfileResponseDTO> getStaffs(Long locationId);
    User updateProfile(String username, UserProfileUpdateDTO dto);
}
