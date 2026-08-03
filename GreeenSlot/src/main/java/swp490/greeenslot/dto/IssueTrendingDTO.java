package swp490.greeenslot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueTrendingDTO {
    
    private String issueType;
    
    private Long totalCount;
    
    private Long currentPeriodCount;
    
    private Long previousPeriodCount;
    
    private Double trendPercentage; // positive = increasing, negative = decreasing
    
    private String trendDirection; // INCREASING, DECREASING, STABLE
    
    private List<TrendDataPoint> trendDataPoints;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String period;
        private Long count;
    }
}
