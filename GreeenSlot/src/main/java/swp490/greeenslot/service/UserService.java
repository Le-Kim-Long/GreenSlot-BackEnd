package swp490.greeenslot.service;

import swp490.greeenslot.dto.UserProfileUpdateDTO;
import swp490.greeenslot.entity.User;

public interface UserService {
    User updateProfile(String username, UserProfileUpdateDTO dto);
}
