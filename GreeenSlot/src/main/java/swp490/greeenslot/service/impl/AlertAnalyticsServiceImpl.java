package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.AlertAnalyticsDTO;
import swp490.greeenslot.entity.Alert;
import swp490.greeenslot.entity.EAlertStatus;
import swp490.greeenslot.repository.AlertRepository;
import swp490.greeenslot.service.AlertAnalyticsService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlertAnalyticsServiceImpl implements AlertAnalyticsService {

    @Autowired
    private AlertRepository alertRepository;

    @Override
    public AlertAnalyticsDTO getAlertAnalytics(Instant startDate, Instant endDate, Long locationId) {
        LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());

        List<Alert> alerts = alertRepository.findAll().stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(start) && a.getCreatedAt().isBefore(end))
                .filter(a -> {
                    if (locationId == null) return true;
                    if (a.getPillar() != null && a.getPillar().getLocation() != null) {
                        return locationId.equals(a.getPillar().getLocation().getId());
                    }
                    if (a.getGardenSlot() != null && a.getGardenSlot().getPillar() != null && a.getGardenSlot().getPillar().getLocation() != null) {
                        return locationId.equals(a.getGardenSlot().getPillar().getLocation().getId());
                    }
                    return false;
                })
                .toList();

        AlertAnalyticsDTO analytics = new AlertAnalyticsDTO();

        // Total alerts
        analytics.setTotalAlerts((long) alerts.size());

        // Pending alerts
        long pendingAlerts = alerts.stream()
                .filter(a -> a.getStatus() == EAlertStatus.PENDING)
                .count();
        analytics.setPendingAlerts(pendingAlerts);

        // Resolved alerts
        long resolvedAlerts = alerts.stream()
                .filter(a -> a.getStatus() == EAlertStatus.RESOLVED)
                .count();
        analytics.setResolvedAlerts(resolvedAlerts);

        // Critical alerts (threshold exceeded by significant margin)
        long criticalAlerts = alerts.stream()
                .filter(a -> a.getActualValue() != null && a.getThresholdValue() != null
                        && Math.abs(a.getActualValue() - a.getThresholdValue()) > (a.getThresholdValue() * 0.5))
                .count();
        analytics.setCriticalAlerts(criticalAlerts);

        // Alerts by type
        Map<String, Long> alertsByType = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getAlertType, Collectors.counting()));
        analytics.setAlertsByType(alertsByType);

        // Alerts by sensor type
        Map<String, Long> alertsBySensorType = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getSensorType, Collectors.counting()));
        analytics.setAlertsBySensorType(alertsBySensorType);

        // Average resolution time (for resolved alerts)
        List<Alert> resolved = alerts.stream()
                .filter(a -> a.getStatus() == EAlertStatus.RESOLVED)
                .toList();
        
        if (!resolved.isEmpty()) {
            long avgResolutionMinutes = resolved.stream()
                    .filter(a -> a.getCreatedAt() != null)
                    .mapToLong(a -> ChronoUnit.MINUTES.between(a.getCreatedAt(), LocalDateTime.now()))
                    .sum() / resolved.size();
            analytics.setAverageResolutionTimeMinutes(avgResolutionMinutes);
        } else {
            analytics.setAverageResolutionTimeMinutes(0L);
        }

        // Most common alert type
        String mostCommonType = alertsByType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
        analytics.setMostCommonAlertType(mostCommonType);

        // Most common alert frequency
        Long mostCommonFrequency = alertsByType.values().stream()
                .max(Long::compareTo)
                .orElse(0L);
        analytics.setMostCommonAlertFrequency(mostCommonFrequency);

        return analytics;
    }
}
