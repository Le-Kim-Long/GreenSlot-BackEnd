package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.ProfileResponseDTO;
import swp490.greeenslot.dto.UserProfileUpdateDTO;
import swp490.greeenslot.entity.ERole;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponseDTO getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return new ProfileResponseDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponseDTO> getStaffs(Long locationId) {
        List<User> staffs;
        if (locationId != null) {
            staffs = userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, locationId);
        } else {
            staffs = userRepository.findByRoleName(ERole.ROLE_GARDEN_STAFF);
        }
        return staffs.stream()
                .map(ProfileResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public User updateProfile(String username, UserProfileUpdateDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress());
        }
        if (dto.getImageUrl() != null) {
            user.setImageUrl(dto.getImageUrl());
        }

        return userRepository.save(user);
    }
}
