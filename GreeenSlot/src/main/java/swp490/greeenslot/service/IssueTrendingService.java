package swp490.greeenslot.service;

import swp490.greeenslot.dto.IssueTrendingDTO;

import java.time.Instant;
import java.util.List;

public interface IssueTrendingService {
    
    List<IssueTrendingDTO> getAlertTrending(Instant startDate, Instant endDate);
    
    List<IssueTrendingDTO> getTaskTrending(Instant startDate, Instant endDate);
}
