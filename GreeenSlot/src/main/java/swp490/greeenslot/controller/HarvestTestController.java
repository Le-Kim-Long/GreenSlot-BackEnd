package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.service.HarvestReminderService;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test/harvest")
@Tag(name = "Harvest Reminder (Test)", description = "Dev-only endpoints to manually test the harvest reminder job without waiting for the daily cron or real growth time")
public class HarvestTestController {

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private TreeRepository treeRepository;

    @Autowired
    private HarvestReminderService harvestReminderService;

    @PostMapping("/backdate/{rentalId}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Backdate a rental's plantedAt so it looks like it was planted N days ago, for testing the harvest reminder. Optionally pass treeId if the rental has no tree assigned yet (e.g. requests approved before this feature existed).")
    public ResponseEntity<Map<String, Object>> backdate(
            @PathVariable Long rentalId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long treeId) {
        SlotRental rental = slotRentalRepository.findById(rentalId)
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + rentalId));

        if (treeId != null) {
            Tree tree = treeRepository.findById(treeId)
                    .orElseThrow(() -> new RuntimeException("Tree not found with id: " + treeId));
            rental.setTree(tree);
        }

        rental.setPlantedAt(LocalDateTime.now().minusDays(days));
        rental.setHarvestReminderSent(false);
        slotRentalRepository.save(rental);
        return ResponseEntity.ok(Map.of(
                "rentalId", rentalId,
                "plantedAt", rental.getPlantedAt().toString(),
                "tree", rental.getTree() != null ? rental.getTree().getTreeName() : "null",
                "harvestDays", rental.getTree() != null ? rental.getTree().getHarvestDays() : -1
        ));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Manually run the harvest reminder job right now instead of waiting for the 8 AM cron")
    public ResponseEntity<String> trigger() {
        harvestReminderService.checkAndNotifyHarvestReady();
        return ResponseEntity.ok("Harvest reminder job executed.");
    }

    @PostMapping("/clear/{rentalId}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Wipe all harvest-related state on a rental (tree, plantedAt, notified, decision) back to empty, as if just harvested. Useful to un-stick test data left over from before the auto-reset-after-harvest fix existed.")
    public ResponseEntity<Map<String, Object>> clear(@PathVariable Long rentalId) {
        SlotRental rental = slotRentalRepository.findById(rentalId)
                .orElseThrow(() -> new RuntimeException("Rental not found with id: " + rentalId));

        rental.setTree(null);
        rental.setTreeStatus(null);
        rental.setTreeNotes(null);
        rental.setPlantedAt(null);
        rental.setHarvestReminderSent(false);
        rental.setHarvestNotifiedAt(null);
        rental.setHarvestDecision(null);
        slotRentalRepository.save(rental);

        return ResponseEntity.ok(Map.of("rentalId", rentalId, "message", "Harvest state cleared."));
    }
}
