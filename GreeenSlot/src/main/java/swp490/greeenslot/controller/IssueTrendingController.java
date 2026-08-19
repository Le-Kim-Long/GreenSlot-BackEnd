package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.IssueTrendingDTO;
import swp490.greeenslot.service.IssueTrendingService;

import java.time.Instant;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/analytics/trending")
@Tag(name = "Issue Trending", description = "APIs for issue trending analysis")
public class IssueTrendingController {

    @Autowired
    private IssueTrendingService issueTrendingService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get alert trending analysis", description = "Returns trending analysis for alerts grouped by type")
    public ResponseEntity<List<IssueTrendingDTO>> getAlertTrending(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        return ResponseEntity.ok(issueTrendingService.getAlertTrending(startDate, endDate));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_LOCATION_MANAGER')")
    @Operation(summary = "Get task trending analysis", description = "Returns trending analysis for tasks grouped by type")
    public ResponseEntity<List<IssueTrendingDTO>> getTaskTrending(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate) {
        return ResponseEntity.ok(issueTrendingService.getTaskTrending(startDate, endDate));
    }
}
