package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.entity.HarvestHistory;
import swp490.greeenslot.service.HarvestHistoryService;

import java.security.Principal;
import java.util.List;

@CrossOrigin(origins = {"https://greenslot-taupe.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/harvest-history")
@Tag(name = "Harvest History", description = "Read-only history of completed harvests")
public class HarvestHistoryController {

    @Autowired
    private HarvestHistoryService harvestHistoryService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Get the current customer's own harvest history")
    public ResponseEntity<List<HarvestHistory>> getMyHistory(Principal principal) {
        return ResponseEntity.ok(harvestHistoryService.getMyHistory(principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_GARDEN_STAFF', 'ROLE_LOCATION_MANAGER', 'ROLE_MANAGER', 'ROLE_ADMIN')")
    @Operation(summary = "Get harvest history for the caller's location (or all locations for global manager/admin)")
    public ResponseEntity<List<HarvestHistory>> getHistoryForManager(Principal principal) {
        return ResponseEntity.ok(harvestHistoryService.getHistoryForManager(principal.getName()));
    }
}
