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
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private GlobalContentRepository globalContentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserAdminDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToUserAdminDTO);
    }

    @Override
    public UserAdminDTO updateUserAuthorities(Long userId, UserAuthorityUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Prevent admin from removing their own ROLE_ADMIN authority
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            boolean hasAdminRole = dto.getRoles().stream()
                    .anyMatch(role -> role.equals("ROLE_ADMIN"));
            if (!hasAdminRole) {
                throw new IllegalArgumentException("Cannot remove ROLE_ADMIN from your own account");
            }
        }

        Set<Role> newRoles = new HashSet<>();
        for (String roleStr : dto.getRoles()) {
            ERole eRole;
            try {
                eRole = ERole.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role name: " + roleStr);
            }

            Role role = roleRepository.findByName(eRole)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found in database: " + roleStr));
            newRoles.add(role);
        }

        user.setRoles(newRoles);
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
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogs(LocalDateTime start, LocalDateTime end) {
        List<AuditLog> logs;
        if (start != null && end != null) {
            logs = auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(start, end);
        } else {
            logs = auditLogRepository.findAllByOrderByPerformedAtDesc();
        }
        return logs.stream().map(this::convertToAuditLogDTO).collect(Collectors.toList());
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
                roles
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
