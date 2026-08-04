package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.IssueTrendingDTO;
import swp490.greeenslot.entity.Alert;
import swp490.greeenslot.entity.GardeningTask;
import swp490.greeenslot.repository.AlertRepository;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.service.IssueTrendingService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IssueTrendingServiceImpl implements IssueTrendingService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Override
    public List<IssueTrendingDTO> getAlertTrending(Instant startDate, Instant endDate) {
        LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());

        List<Alert> alerts = alertRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().isAfter(start) && a.getCreatedAt().isBefore(end))
                .toList();

        // Group by alert type
        Map<String, List<Alert>> alertsByType = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getAlertType));

        List<IssueTrendingDTO> trendingList = new ArrayList<>();

        for (Map.Entry<String, List<Alert>> entry : alertsByType.entrySet()) {
            String issueType = entry.getKey();
            List<Alert> typeAlerts = entry.getValue();

            // Split into two periods for comparison
            long totalDays = ChronoUnit.DAYS.between(start, end);
            LocalDateTime midPoint = start.plusDays(totalDays / 2);

            long currentPeriodCount = typeAlerts.stream()
                    .filter(a -> a.getCreatedAt().isAfter(midPoint) || a.getCreatedAt().isEqual(midPoint))
                    .count();

            long previousPeriodCount = typeAlerts.stream()
                    .filter(a -> a.getCreatedAt().isBefore(midPoint))
                    .count();

            // Calculate trend percentage
            double trendPercentage = 0.0;
            if (previousPeriodCount > 0) {
                trendPercentage = ((double) (currentPeriodCount - previousPeriodCount) / previousPeriodCount) * 100;
            } else if (currentPeriodCount > 0) {
                trendPercentage = 100.0; // New issue type
            }

            String trendDirection = Math.abs(trendPercentage) < 5 ? "STABLE" 
                    : trendPercentage > 0 ? "INCREASING" : "DECREASING";

            // Create trend data points (weekly)
            List<IssueTrendingDTO.TrendDataPoint> trendDataPoints = createWeeklyTrendDataPoints(typeAlerts, start, end);

            IssueTrendingDTO trending = new IssueTrendingDTO();
            trending.setIssueType(issueType);
            trending.setTotalCount((long) typeAlerts.size());
            trending.setCurrentPeriodCount(currentPeriodCount);
            trending.setPreviousPeriodCount(previousPeriodCount);
            trending.setTrendPercentage(trendPercentage);
            trending.setTrendDirection(trendDirection);
            trending.setTrendDataPoints(trendDataPoints);

            trendingList.add(trending);
        }

        // Sort by total count descending
        trendingList.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));

        return trendingList;
    }

    @Override
    public List<IssueTrendingDTO> getTaskTrending(Instant startDate, Instant endDate) {
        LocalDateTime start = LocalDateTime.ofInstant(startDate, java.time.ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(endDate, java.time.ZoneId.systemDefault());

        List<GardeningTask> tasks = gardeningTaskRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(start) && t.getCreatedAt().isBefore(end))
                .toList();

        // Group by task type
        Map<String, List<GardeningTask>> tasksByType = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getTaskType().name()));

        List<IssueTrendingDTO> trendingList = new ArrayList<>();

        for (Map.Entry<String, List<GardeningTask>> entry : tasksByType.entrySet()) {
            String issueType = entry.getKey();
            List<GardeningTask> typeTasks = entry.getValue();

            // Split into two periods for comparison
            long totalDays = ChronoUnit.DAYS.between(start, end);
            LocalDateTime midPoint = start.plusDays(totalDays / 2);

            long currentPeriodCount = typeTasks.stream()
                    .filter(t -> t.getCreatedAt().isAfter(midPoint) || t.getCreatedAt().isEqual(midPoint))
                    .count();

            long previousPeriodCount = typeTasks.stream()
                    .filter(t -> t.getCreatedAt().isBefore(midPoint))
                    .count();

            // Calculate trend percentage
            double trendPercentage = 0.0;
            if (previousPeriodCount > 0) {
                trendPercentage = ((double) (currentPeriodCount - previousPeriodCount) / previousPeriodCount) * 100;
            } else if (currentPeriodCount > 0) {
                trendPercentage = 100.0;
            }

            String trendDirection = Math.abs(trendPercentage) < 5 ? "STABLE" 
                    : trendPercentage > 0 ? "INCREASING" : "DECREASING";

            // Create trend data points (weekly)
            List<IssueTrendingDTO.TrendDataPoint> trendDataPoints = createWeeklyTrendDataPointsForTasks(typeTasks, start, end);

            IssueTrendingDTO trending = new IssueTrendingDTO();
            trending.setIssueType(issueType);
            trending.setTotalCount((long) typeTasks.size());
            trending.setCurrentPeriodCount(currentPeriodCount);
            trending.setPreviousPeriodCount(previousPeriodCount);
            trending.setTrendPercentage(trendPercentage);
            trending.setTrendDirection(trendDirection);
            trending.setTrendDataPoints(trendDataPoints);

            trendingList.add(trending);
        }

        // Sort by total count descending
        trendingList.sort((a, b) -> Long.compare(b.getTotalCount(), a.getTotalCount()));

        return trendingList;
    }

    private List<IssueTrendingDTO.TrendDataPoint> createWeeklyTrendDataPoints(List<Alert> alerts, LocalDateTime start, LocalDateTime end) {
        List<IssueTrendingDTO.TrendDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime current = start;
        while (current.isBefore(end)) {
            LocalDateTime weekEnd = current.plusDays(7);
            if (weekEnd.isAfter(end)) {
                weekEnd = end;
            }

            final LocalDateTime weekStart = current;
            final LocalDateTime weekEndFinal = weekEnd;

            long count = alerts.stream()
                    .filter(a -> !a.getCreatedAt().isBefore(weekStart) && !a.getCreatedAt().isAfter(weekEndFinal))
                    .count();

            dataPoints.add(new IssueTrendingDTO.TrendDataPoint(
                    current.format(formatter),
                    count
            ));

            current = weekEnd.plusDays(1);
        }

        return dataPoints;
    }

    private List<IssueTrendingDTO.TrendDataPoint> createWeeklyTrendDataPointsForTasks(List<GardeningTask> tasks, LocalDateTime start, LocalDateTime end) {
        List<IssueTrendingDTO.TrendDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime current = start;
        while (current.isBefore(end)) {
            LocalDateTime weekEnd = current.plusDays(7);
            if (weekEnd.isAfter(end)) {
                weekEnd = end;
            }

            final LocalDateTime weekStart = current;
            final LocalDateTime weekEndFinal = weekEnd;

            long count = tasks.stream()
                    .filter(t -> !t.getCreatedAt().isBefore(weekStart) && !t.getCreatedAt().isAfter(weekEndFinal))
                    .count();

            dataPoints.add(new IssueTrendingDTO.TrendDataPoint(
                    current.format(formatter),
                    count
            ));

            current = weekEnd.plusDays(1);
        }

        return dataPoints;
    }
}
