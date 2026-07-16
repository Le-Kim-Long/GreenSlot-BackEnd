package swp490.greeenslot.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp490.greeenslot.dto.*;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {
    Page<UserAdminDTO> getAllUsers(Pageable pageable);
    UserAdminDTO updateUserAuthorities(Long userId, UserAuthorityUpdateDTO dto);
    UserAdminDTO updateUserStatus(Long userId, UserStatusUpdateDTO dto);
    org.springframework.data.domain.Page<swp490.greeenslot.dto.AuditLogDTO> getAuditLogs(java.time.LocalDateTime start, java.time.LocalDateTime end, org.springframework.data.domain.Pageable pageable);
    GlobalContentDTO createContent(GlobalContentDTO dto);
    GlobalContentDTO updateContent(Long id, GlobalContentDTO dto);
    List<GlobalContentDTO> getAllContent();
}
