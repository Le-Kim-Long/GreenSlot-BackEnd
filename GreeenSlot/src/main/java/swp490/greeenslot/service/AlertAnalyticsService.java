package swp490.greeenslot.service;

import swp490.greeenslot.dto.AlertAnalyticsDTO;

import java.time.Instant;

public interface AlertAnalyticsService {
    
    AlertAnalyticsDTO getAlertAnalytics(Instant startDate, Instant endDate, Long locationId);

    default AlertAnalyticsDTO getAlertAnalytics(Instant startDate, Instant endDate) {
        return getAlertAnalytics(startDate, endDate, null);
    }
}
