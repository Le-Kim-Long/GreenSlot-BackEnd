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
    private NotificationService notificationService;

    @Autowired(required = false)
    private FirebaseMessagingService firebaseMessagingService;

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
        return alertRepository.findByStatusOrderByCreatedAtDesc(EAlertStatus.PENDING).stream()
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
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
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
        }
        alertRepository.save(alert);

        return mapToLogDTO(savedLog);
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
