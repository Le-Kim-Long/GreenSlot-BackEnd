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
    List<AuditLogDTO> getAuditLogs(LocalDateTime start, LocalDateTime end);
    GlobalContentDTO createContent(GlobalContentDTO dto);
    GlobalContentDTO updateContent(Long id, GlobalContentDTO dto);
    List<GlobalContentDTO> getAllContent();
}
