package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.CustomerLifetimeValueDTO;
import swp490.greeenslot.dto.SensorAggregateDTO;
import swp490.greeenslot.entity.ESensorType;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.repository.GardenSlotRepository;
import swp490.greeenslot.service.CustomerAnalyticsService;
import swp490.greeenslot.service.SensorReadingService;

import java.util.List;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Customer Analytics", description = "APIs for customer analytics and lifetime value calculations")
public class CustomerAnalyticsController {

    @Autowired
    private CustomerAnalyticsService customerAnalyticsService;

    @Autowired
    private SensorReadingService sensorReadingService;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    /** Ô vườn (GardenSlot) va tru vuon (Pillar) la hai ID khac nhau — phai resolve qua GardenSlot truoc khi truy van cam bien theo tru. */
    private Long resolvePillarId(Long slotId) {
        GardenSlot slot = gardenSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found with ID: " + slotId));
        if (slot.getPillar() == null) {
            throw new IllegalArgumentException("Slot " + slotId + " is not associated with a pillar");
        }
        return slot.getPillar().getId();
    }

    @GetMapping("/customers/{userId}/clv")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Operation(summary = "Calculate customer lifetime value", description = "Calculates the lifetime value for a specific customer")
    public ResponseEntity<CustomerLifetimeValueDTO> calculateCLV(@PathVariable Long userId) {
        return ResponseEntity.ok(customerAnalyticsService.calculateCustomerLifetimeValue(userId));
    }

    @GetMapping("/customers/clv")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MANAGER')")
    @Operation(summary = "Get all customer lifetime values", description = "Returns lifetime value metrics for all customers")
    public ResponseEntity<List<CustomerLifetimeValueDTO>> getAllCLVs() {
        return ResponseEntity.ok(customerAnalyticsService.getAllCustomerLifetimeValues());
    }

    @GetMapping("/slots/{slotId}/charts/hourly")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get hourly sensor data for charts", description = "Returns hourly aggregated sensor data for the last N hours")
    public ResponseEntity<List<SensorAggregateDTO>> getHourlyChartData(
            @PathVariable Long slotId,
            @RequestParam(required = false) String sensorType,
            @RequestParam(defaultValue = "24") int hours) {
        ESensorType type = sensorType != null ? ESensorType.fromCode(sensorType) : ESensorType.SOIL_MOISTURE;
        return ResponseEntity.ok(sensorReadingService.getHourlyAggregates(resolvePillarId(slotId), type, hours));
    }

    @GetMapping("/slots/{slotId}/charts/daily")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get daily sensor data for charts", description = "Returns daily aggregated sensor data for the last N days")
    public ResponseEntity<List<SensorAggregateDTO>> getDailyChartData(
            @PathVariable Long slotId,
            @RequestParam(required = false) String sensorType,
            @RequestParam(defaultValue = "30") int days) {
        ESensorType type = sensorType != null ? ESensorType.fromCode(sensorType) : ESensorType.SOIL_MOISTURE;
        return ResponseEntity.ok(sensorReadingService.getDailyAggregates(resolvePillarId(slotId), type, days));
    }

    @GetMapping("/slots/{slotId}/charts/weekly")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get weekly sensor data for charts", description = "Returns weekly aggregated sensor data for the last N weeks")
    public ResponseEntity<List<SensorAggregateDTO>> getWeeklyChartData(
            @PathVariable Long slotId,
            @RequestParam(required = false) String sensorType,
            @RequestParam(defaultValue = "12") int weeks) {
        ESensorType type = sensorType != null ? ESensorType.fromCode(sensorType) : ESensorType.SOIL_MOISTURE;
        return ResponseEntity.ok(sensorReadingService.getWeeklyAggregates(resolvePillarId(slotId), type, weeks));
    }
}


