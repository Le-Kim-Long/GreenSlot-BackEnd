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

    @Override
    public List<AlertDTO> getAllAlerts() {
        return alertRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AlertDTO getAlertById(Long id) {
        return alertRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
    }

    @Override
    public List<AlertDTO> getAlertsByStatus(String status) {
        EAlertStatus alertStatus = EAlertStatus.valueOf(status.toUpperCase());
        return alertRepository.findByStatus(alertStatus).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDTO> getAlertsByPillar(Long pillarId) {
        Pillar pillar = pillarRepository.findById(pillarId)
                .orElseThrow(() -> new RuntimeException("Pillar not found with id: " + pillarId));
        return alertRepository.findByPillar(pillar).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertDTO> getPendingAlerts() {
        return alertRepository.findByStatusOrderByCreatedAtDesc(EAlertStatus.PENDING).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertProcessingLogDTO processAlert(AlertProcessingRequestDTO request, String username) {
        Alert alert = alertRepository.findById(request.getAlertId())
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + request.getAlertId()));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        
        AlertProcessingLog log = new AlertProcessingLog();
        log.setAlert(alert);
        log.setProcessedBy(user);
        log.setStatus(EAlertProcessingStatus.valueOf(request.getStatus().toUpperCase()));
        log.setComment(request.getComment());
        log.setEvidenceImageUrl(request.getEvidenceImageUrl());
        
        AlertProcessingLog savedLog = alertProcessingLogRepository.save(log);
        
        alert.setStatus(EAlertStatus.valueOf(request.getStatus().toUpperCase()));
        if (EAlertStatus.valueOf(request.getStatus().toUpperCase()) == EAlertStatus.RESOLVED) {
            alert.setResolvedAt(LocalDateTime.now());
        }
        alertRepository.save(alert);
        
        return mapToLogDTO(savedLog);
    }

    @Override
    public List<AlertProcessingLogDTO> getAlertProcessingLogs(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + alertId));
        return alertProcessingLogRepository.findByAlert(alert).stream()
                .map(this::mapToLogDTO)
                .collect(Collectors.toList());
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
