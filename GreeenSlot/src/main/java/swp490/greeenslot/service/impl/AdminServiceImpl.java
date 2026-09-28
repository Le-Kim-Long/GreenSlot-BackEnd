package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.*;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.AdminService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private GlobalContentRepository globalContentRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public UserAdminDTO createUser(UserAdminCreateDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setEnabled(true);

        Set<Role> roles = new HashSet<>();
        ERole assignedRole = ERole.ROLE_CUSTOMER;
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            String roleStr = dto.getRoles().get(0);
            try {
                assignedRole = ERole.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Error: Role " + roleStr + " is invalid.");
            }
        }
        final ERole finalRole = assignedRole;
        Role roleEntity = roleRepository.findByName(finalRole)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(roleEntity);
        user.setRoles(roles);

        // Only assign location for ROLE_LOCATION_MANAGER or ROLE_GARDEN_STAFF
        boolean canHaveLocation = (assignedRole == ERole.ROLE_LOCATION_MANAGER || assignedRole == ERole.ROLE_GARDEN_STAFF);
        if (canHaveLocation && dto.getLocationId() != null && dto.getLocationId() > 0) {
            Location location = locationRepository.findById(dto.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException("Location not found"));
            user.setLocation(location);
        } else {
            user.setLocation(null);
        }

        User savedUser = userRepository.save(user);
        return convertToUserAdminDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserAdminDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToUserAdminDTO);
    }

    @Override
    public UserAdminDTO updateUserAuthorities(Long userId, UserAuthorityUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn 1 vai trò cho người dùng.");
        }

        String roleStr = dto.getRoles().get(0);
        ERole eRole;
        try {
            eRole = ERole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role name: " + roleStr);
        }

        // Prevent admin from removing their own ROLE_ADMIN authority
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername) && eRole != ERole.ROLE_ADMIN) {
            throw new IllegalArgumentException("Cannot remove ROLE_ADMIN from your own account");
        }

        Role role = roleRepository.findByName(eRole)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleStr));
        Set<Role> newRoles = new HashSet<>();
        newRoles.add(role);
        user.setRoles(newRoles);

        // Clear location if role is NOT location_manager or garden_staff
        boolean canHaveLocation = (eRole == ERole.ROLE_LOCATION_MANAGER || eRole == ERole.ROLE_GARDEN_STAFF);
        if (!canHaveLocation) {
            user.setLocation(null);
        }

        User updatedUser = userRepository.save(user);
        return convertToUserAdminDTO(updatedUser);
    }

    @Override
    public UserAdminDTO updateUserStatus(Long userId, UserStatusUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Prevent admin from disabling their own account
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername) && !dto.getEnabled()) {
            throw new IllegalArgumentException("Cannot disable your own account");
        }

        user.setEnabled(dto.getEnabled());
        User updatedUser = userRepository.save(user);
        return convertToUserAdminDTO(updatedUser);
    }

    @Override
    public UserAdminDTO updateUserLocation(Long userId, Long locationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        boolean canHaveLocation = user.getRoles() != null && user.getRoles().stream().anyMatch(r ->
                r.getName() == ERole.ROLE_LOCATION_MANAGER || r.getName() == ERole.ROLE_GARDEN_STAFF);

        if (!canHaveLocation || locationId == null || locationId == 0) {
            user.setLocation(null);
        } else {
            Location location = locationRepository.findById(locationId)
                    .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + locationId));
            user.setLocation(location);
        }

        User updatedUser = userRepository.save(user);
        return convertToUserAdminDTO(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogs(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Page<AuditLog> logs;
        if (start != null && end != null) {
            logs = auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(start, end, pageable);
        } else {
            logs = auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
        }
        return logs.map(this::convertToAuditLogDTO);
    }

    @Override
    public GlobalContentDTO createContent(GlobalContentDTO dto) {
        GlobalContent content = new GlobalContent();
        content.setTitle(dto.getTitle());
        content.setContent(dto.getContent());
        content.setContentType(dto.getContentType());
        content.setActive(dto.getActive() != null ? dto.getActive() : true);

        GlobalContent savedContent = globalContentRepository.save(content);
        return convertToGlobalContentDTO(savedContent);
    }

    @Override
    public GlobalContentDTO updateContent(Long id, GlobalContentDTO dto) {
        GlobalContent content = globalContentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Content not found with id: " + id));

        content.setTitle(dto.getTitle());
        content.setContent(dto.getContent());
        content.setContentType(dto.getContentType());
        if (dto.getActive() != null) {
            content.setActive(dto.getActive());
        }

        GlobalContent updatedContent = globalContentRepository.save(content);
        return convertToGlobalContentDTO(updatedContent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GlobalContentDTO> getAllContent() {
        return globalContentRepository.findAll().stream()
                .map(this::convertToGlobalContentDTO)
                .collect(Collectors.toList());
    }

    private UserAdminDTO convertToUserAdminDTO(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        return new UserAdminDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getEnabled() != null ? user.getEnabled() : true,
                roles,
                user.getLocation() != null ? user.getLocation().getId() : null,
                user.getLocation() != null ? user.getLocation().getName() : null
        );
    }

    private AuditLogDTO convertToAuditLogDTO(AuditLog auditLog) {
        return new AuditLogDTO(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getPerformedBy(),
                auditLog.getPerformedAt(),
                auditLog.getDetails(),
                auditLog.getIpAddress()
        );
    }

    private GlobalContentDTO convertToGlobalContentDTO(GlobalContent content) {
        return new GlobalContentDTO(
                content.getId(),
                content.getTitle(),
                content.getContent(),
                content.getContentType(),
                content.getActive()
        );
    }
}
