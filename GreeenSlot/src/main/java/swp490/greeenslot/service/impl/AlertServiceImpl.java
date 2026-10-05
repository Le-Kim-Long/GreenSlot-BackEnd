package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.AlertDTO;
import swp490.greeenslot.dto.AlertProcessingLogDTO;
import swp490.greeenslot.dto.AlertProcessingRequestDTO;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.AlertService;
import swp490.greeenslot.service.FirebaseMessagingService;
import swp490.greeenslot.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertProcessingLogRepository alertProcessingLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private TreeRepository treeRepository;

    @Autowired(required = false)
    private SlotRentalRepository slotRentalRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired(required = false)
    private FirebaseMessagingService firebaseMessagingService;

    @Autowired(required = false)
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired(required = false)
    private swp490.greeenslot.service.LocationContextService locationContextService;

    private Long getAlertLocationId(Alert alert) {
        if (alert == null) return null;
        if (alert.getPillar() != null && alert.getPillar().getLocation() != null) {
            return alert.getPillar().getLocation().getId();
        }
        if (alert.getGardenSlot() != null && alert.getGardenSlot().getPillar() != null && alert.getGardenSlot().getPillar().getLocation() != null) {
            return alert.getGardenSlot().getPillar().getLocation().getId();
        }
        return null;
    }

    private boolean isAlertAccessible(Alert alert, Long locationId) {
        if (locationId == null) return true;
        Long alertLocId = getAlertLocationId(alert);
        return alertLocId == null || alertLocId.equals(locationId);
    }

    @Override
    public List<AlertDTO> getAllAlerts() {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return alertRepository.findAll().stream()
                .filter(a -> isAlertAccessible(a, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AlertDTO getAlertById(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
        if (locationContextService != null) {
            Long alertLocId = getAlertLocationId(alert);
            locationContextService.validateLocationAccess(alertLocId);
        }
        return mapToDTO(alert);
    }

    @Override
    public List<AlertDTO> getAlertsByStatus(String status) {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        EAlertStatus alertStatus = EAlertStatus.valueOf(status.toUpperCase());
        return alertRepository.findByStatus(alertStatus).stream()
                .filter(a -> isAlertAccessible(a, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDTO> getAlertsByPillar(Long pillarId) {
        Pillar pillar = pillarRepository.findById(pillarId)
                .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + pillarId));
        if (pillar.getLocation() != null && locationContextService != null) {
            locationContextService.validateLocationAccess(pillar.getLocation().getId());
        }
        return alertRepository.findByPillar(pillar).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDTO> getPendingAlerts() {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return alertRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(EAlertStatus.PENDING, EAlertStatus.IN_PROGRESS, EAlertStatus.ESCALATED)
        ).stream()
                .filter(a -> isAlertAccessible(a, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertProcessingLogDTO processAlert(AlertProcessingRequestDTO request, String username) {
        Alert alert = alertRepository.findById(request.getAlertId())
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + request.getAlertId()));
        if (locationContextService != null) {
            Long alertLocId = getAlertLocationId(alert);
            locationContextService.validateLocationAccess(alertLocId);
        }
        
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found with username/email: " + username));
        
        EAlertStatus newAlertStatus = EAlertStatus.valueOf(request.getStatus().toUpperCase());

        AlertProcessingLog log = new AlertProcessingLog();
        log.setAlert(alert);
        log.setProcessedBy(user);
        log.setStatus(toProcessingStatus(newAlertStatus));
        log.setComment(request.getComment());
        log.setEvidenceImageUrl(request.getEvidenceImageUrl());

        AlertProcessingLog savedLog = alertProcessingLogRepository.save(log);

        alert.setStatus(newAlertStatus);
        if (newAlertStatus == EAlertStatus.RESOLVED) {
            alert.setResolvedAt(LocalDateTime.now());
            String evidenceForTask = hasText(request.getEvidenceImageUrl())
                    ? request.getEvidenceImageUrl() : findLatestEvidenceImage(alert);
            // Tự động hoàn thành các GardeningTask khẩn cấp tương ứng cho ô đất này
            for (GardeningTask t : findOpenAlertTasks(alert)) {
                t.setStatus(ETaskStatus.COMPLETED);
                if (t.getStaffNotes() == null || t.getStaffNotes().isBlank()) {
                    t.setStaffNotes("Đã xử lý cảnh báo IoT: " + (request.getComment() != null ? request.getComment() : "Hoàn tất"));
                }
                if (!hasText(t.getEvidenceImageUrl()) && evidenceForTask != null) {
                    t.setEvidenceImageUrl(evidenceForTask);
                }
                gardeningTaskRepository.save(t);
            }

            // Gửi thông báo đến khách hàng sở hữu ô đất có cảnh báo được xử lý
            if (notificationService != null) {
                notifyCustomerAlertResolved(alert, evidenceForTask);
            }
        }
        alertRepository.save(alert);

        // Gửi thông báo đến Manager / Location Manager khi Garden Staff gửi báo cáo khắc phục
        boolean isStaff = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_GARDEN_STAFF);
        if (isStaff && newAlertStatus == EAlertStatus.IN_PROGRESS && hasText(request.getEvidenceImageUrl())) {
            // Gắn ảnh hiện trường vào task khẩn cấp để Quản lý xem được cả ở trang Quản lý công việc
            for (GardeningTask t : findOpenAlertTasks(alert)) {
                t.setEvidenceImageUrl(request.getEvidenceImageUrl());
                gardeningTaskRepository.save(t);
            }
        }
        if (isStaff && newAlertStatus == EAlertStatus.IN_PROGRESS && notificationService != null) {
            Long locId = getAlertLocationId(alert);
            String pillarCode = (alert.getPillar() != null && alert.getPillar().getPillarCode() != null)
                    ? alert.getPillar().getPillarCode() : ("#" + alert.getId());
            String title = "Báo cáo xử lý sự cố Trụ " + pillarCode;
            String message = user.getFullName() + " đã gửi báo cáo khắc phục cảnh báo Trụ " + pillarCode + ": \""
                    + (request.getComment() != null ? request.getComment() : "") + "\". Vui lòng kiểm tra và nghiệm thu.";

            java.util.Set<User> managersToNotify = new java.util.LinkedHashSet<>();
            if (locId != null) {
                managersToNotify.addAll(userRepository.findByRoleNameAndLocation(ERole.ROLE_LOCATION_MANAGER, locId));
            }
            managersToNotify.addAll(userRepository.findByRoleName(ERole.ROLE_MANAGER));
            for (User mgr : managersToNotify) {
                notificationService.createNotification(
                        mgr.getId(),
                        title,
                        message,
                        "ALERT_PROCESSED_BY_STAFF",
                        alert.getId(),
                        "/dashboard/staff/alert-processing",
                        request.getEvidenceImageUrl()
                );
            }
        }

        return mapToLogDTO(savedLog);
    }

    @Override
    @Transactional
    public int batchProcessAlerts(List<Long> alertIds, String status, String comment, String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found with username/email: " + username));
        
        EAlertStatus newAlertStatus = (status != null && !status.isBlank()) 
                ? EAlertStatus.valueOf(status.toUpperCase()) 
                : EAlertStatus.RESOLVED;

        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        
        List<Alert> targets;
        if (alertIds != null && !alertIds.isEmpty()) {
            targets = alertRepository.findAllById(alertIds).stream()
                    .filter(a -> isAlertAccessible(a, targetLocationId))
                    .toList();
        } else {
            targets = alertRepository.findByStatusInOrderByCreatedAtDesc(
                    List.of(EAlertStatus.PENDING, EAlertStatus.IN_PROGRESS)
            ).stream()
                    .filter(a -> isAlertAccessible(a, targetLocationId))
                    .toList();
        }

        LocalDateTime now = LocalDateTime.now();
        String safeComment = (comment != null && !comment.isBlank()) ? comment.trim() : "Đã xử lý hàng loạt";

        for (Alert alert : targets) {
            AlertProcessingLog log = new AlertProcessingLog();
            log.setAlert(alert);
            log.setProcessedBy(user);
            log.setStatus(toProcessingStatus(newAlertStatus));
            log.setComment(safeComment);
            alertProcessingLogRepository.save(log);

            alert.setStatus(newAlertStatus);
            if (newAlertStatus == EAlertStatus.RESOLVED) {
                alert.setResolvedAt(now);
                String evidenceForTask = findLatestEvidenceImage(alert);
                for (GardeningTask t : findOpenAlertTasks(alert)) {
                    t.setStatus(ETaskStatus.COMPLETED);
                    if (t.getStaffNotes() == null || t.getStaffNotes().isBlank()) {
                        t.setStaffNotes("Đã xử lý cảnh báo IoT (Hàng loạt): " + safeComment);
                    }
                    if (!hasText(t.getEvidenceImageUrl()) && evidenceForTask != null) {
                        t.setEvidenceImageUrl(evidenceForTask);
                    }
                    gardeningTaskRepository.save(t);
                }
            }
            alertRepository.save(alert);
        }

        // Gửi thông báo đến khách hàng sở hữu ô đất có cảnh báo được xử lý
        if (newAlertStatus == EAlertStatus.RESOLVED && notificationService != null) {
            notifyCustomersBatchAlertResolved(targets);
        }

        return targets.size();
    }

    private void notifyCustomerAlertResolved(Alert alert, String evidenceImageUrl) {
        if (alert == null || slotRentalRepository == null || notificationService == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SlotRental rental = null;
        if (alert.getPillar() != null) {
            rental = slotRentalRepository.findActiveRentalsByPillarIds(List.of(alert.getPillar().getId()), now).stream()
                    .filter(r -> r.getStatus() == ERentalStatus.ACTIVE && r.getUser() != null)
                    .findFirst().orElse(null);
        }
        if (rental == null && alert.getGardenSlot() != null) {
            rental = slotRentalRepository.findByGardenSlotId(alert.getGardenSlot().getId()).stream()
                    .filter(r -> r.getStatus() == ERentalStatus.ACTIVE && r.getUser() != null)
                    .filter(r -> r.getEndTime() == null || r.getEndTime().isAfter(now))
                    .findFirst().orElse(null);
        }
        if (rental == null) {
            return;
        }
        User customer = rental.getUser();
        GardenSlot slot = alert.getGardenSlot() != null ? alert.getGardenSlot() : rental.getGardenSlot();
        String slotNumber = slot == null ? "N/A" : (slot.getSlotNumber() != null ? slot.getSlotNumber() : ("#" + slot.getId()));
        String pillarCode = (alert.getPillar() != null && alert.getPillar().getPillarCode() != null)
                ? alert.getPillar().getPillarCode() : "";
        String pillarInfo = pillarCode.isEmpty() ? "" : " (Trụ " + pillarCode + ")";

        String title = "Cảnh báo cảm biến đã được xử lý";
        String message = String.format("Sự cố cảnh báo tại ô đất %s%s đã được kiểm tra và xử lý thành công.", slotNumber, pillarInfo);

        String evidenceImg = evidenceImageUrl;
        if (evidenceImg != null && evidenceImg.contains(",")) {
            evidenceImg = evidenceImg.split(",")[0].trim();
        }
        if (evidenceImg != null && evidenceImg.length() > 1000) {
            evidenceImg = evidenceImg.substring(0, 1000);
        }

        notificationService.createNotification(
                customer.getId(),
                title,
                message,
                "ALERT_RESOLVED",
                alert.getId(),
                "/dashboard/customer/monitoring",
                evidenceImg
        );

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(customer.getId(), title, message);
        }
    }

    private void notifyCustomersBatchAlertResolved(List<Alert> alerts) {
        if (alerts == null || alerts.isEmpty() || slotRentalRepository == null || notificationService == null) {
            return;
        }
        java.util.Set<String> notifiedKeys = new java.util.HashSet<>();
        for (Alert a : alerts) {
            String key = a.getPillar() != null ? "P" + a.getPillar().getId()
                    : (a.getGardenSlot() != null ? "S" + a.getGardenSlot().getId() : null);
            if (key != null && notifiedKeys.add(key)) {
                notifyCustomerAlertResolved(a, null);
            }
        }
    }

    // Task khẩn cấp (MAINTENANCE, tên chứa "cảnh báo") còn mở của ô đất gắn với cảnh báo
    private List<GardeningTask> findOpenAlertTasks(Alert alert) {
        if (alert.getGardenSlot() == null || gardeningTaskRepository == null) {
            return List.of();
        }
        return gardeningTaskRepository.findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(
                        alert.getGardenSlot().getId(), ETaskType.MAINTENANCE).stream()
                .filter(t -> t.getStatus() != ETaskStatus.COMPLETED && t.getStatus() != ETaskStatus.CANCELLED)
                .filter(t -> t.getTaskName() != null
                        && (t.getTaskName().contains("cảnh báo") || t.getTaskName().contains("Cảnh báo")))
                .toList();
    }

    // Ảnh hiện trường mới nhất nhân viên đã gửi cho cảnh báo này (nếu có)
    private String findLatestEvidenceImage(Alert alert) {
        return alertProcessingLogRepository.findByAlert(alert).stream()
                .filter(l -> hasText(l.getEvidenceImageUrl()))
                .max(java.util.Comparator.comparing(AlertProcessingLog::getId))
                .map(AlertProcessingLog::getEvidenceImageUrl)
                .orElse(null);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Log xử lý (AlertProcessingLog) dùng enum riêng EAlertProcessingStatus (PROCESSED/NOT_PROCESSED/FAILED),
    // khác với trạng thái của Alert (EAlertStatus) — cần map thủ công thay vì valueOf() chung 1 chuỗi cho cả 2 enum
    private EAlertProcessingStatus toProcessingStatus(EAlertStatus alertStatus) {
        return switch (alertStatus) {
            case RESOLVED -> EAlertProcessingStatus.PROCESSED;
            case FAILED -> EAlertProcessingStatus.FAILED;
            default -> EAlertProcessingStatus.NOT_PROCESSED;
        };
    }

    @Override
    public List<AlertProcessingLogDTO> getAlertProcessingLogs(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        if (locationContextService != null) {
            Long alertLocId = getAlertLocationId(alert);
            locationContextService.validateLocationAccess(alertLocId);
        }
        return alertProcessingLogRepository.findByAlert(alert).stream()
                .map(this::mapToLogDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Alert createAlert(Alert alert) {
        return alertRepository.save(alert);
    }

    @Override
    @Transactional
    public Alert createAlertForTreeAndSlot(Alert alert, Long slotId, Long treeId) {
        if (slotId != null) {
            gardenSlotRepository.findById(slotId).ifPresent(slot -> {
                alert.setGardenSlot(slot);
                if (alert.getPillar() == null && slot.getPillar() != null) {
                    alert.setPillar(slot.getPillar());
                }
            });
        }
        if (treeId != null) {
            treeRepository.findById(treeId).ifPresent(alert::setTree);
        }
        return alertRepository.save(alert);
    }

    @Override
    public List<AlertDTO> getAlertsByTree(Long treeId) {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return alertRepository.findByTreeId(treeId).stream()
                .filter(a -> isAlertAccessible(a, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDTO> getAlertsBySlot(Long slotId) {
        Long targetLocationId = locationContextService != null ? locationContextService.resolveTargetLocationId(null) : null;
        return alertRepository.findByGardenSlotId(slotId).stream()
                .filter(a -> isAlertAccessible(a, targetLocationId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertDTO escalateAlert(Long alertId, Long escalateToUserId, String reason) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        if (locationContextService != null) {
            Long alertLocId = getAlertLocationId(alert);
            locationContextService.validateLocationAccess(alertLocId);
        }

        User escalateToUser = userRepository.findById(escalateToUserId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + escalateToUserId));

        alert.setEscalatedToUser(escalateToUser);
        alert.setEscalatedAt(LocalDateTime.now());
        alert.setEscalationReason(reason);
        alert.setEscalationStatus(EAlertStatus.ESCALATED);

        Alert savedAlert = alertRepository.save(alert);

        String title = "Cảnh báo IoT được chuyển tiếp";
        String message = String.format("Cảnh báo #%d (%s) đã được chuyển tiếp đến bạn. Lý do: %s",
                savedAlert.getId(),
                savedAlert.getAlertType() != null ? savedAlert.getAlertType() : "Sự cố cảm biến",
                reason != null ? reason : "Cần xử lý khẩn cấp");

        if (notificationService != null) {
            notificationService.createNotification(
                    escalateToUserId,
                    title,
                    message,
                    "ALERT_ESCALATED",
                    savedAlert.getId(),
                    "/dashboard/staff/alert-processing"
            );
        }

        if (firebaseMessagingService != null) {
            firebaseMessagingService.sendPushNotification(
                    escalateToUserId,
                    title,
                    message
            );
        }

        return mapToDTO(savedAlert);
    }

    private AlertDTO mapToDTO(Alert alert) {
        return new AlertDTO(
                alert.getId(),
                alert.getAlertType(),
                alert.getDescription(),
                alert.getStatus() != null ? alert.getStatus().name() : null,
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getSensorType(),
                alert.getPillar() != null ? alert.getPillar().getId() : null,
                alert.getPillar() != null ? alert.getPillar().getPillarCode() : null,
                alert.getGardenSlot() != null ? alert.getGardenSlot().getId() : null,
                alert.getGardenSlot() != null ? alert.getGardenSlot().getSlotNumber() : null,
                alert.getTree() != null ? alert.getTree().getId() : null,
                alert.getTree() != null ? alert.getTree().getTreeName() : null,
                alert.getCreatedAt(),
                alert.getResolvedAt()
        );
    }

    private AlertProcessingLogDTO mapToLogDTO(AlertProcessingLog log) {
        return new AlertProcessingLogDTO(
                log.getId(),
                log.getAlert() != null ? log.getAlert().getId() : null,
                log.getProcessedBy() != null ? log.getProcessedBy().getId() : null,
                log.getProcessedBy() != null ? log.getProcessedBy().getFullName() : null,
                log.getStatus() != null ? log.getStatus().name() : null,
                log.getComment(),
                log.getEvidenceImageUrl(),
                log.getProcessedAt()
        );
    }
}
