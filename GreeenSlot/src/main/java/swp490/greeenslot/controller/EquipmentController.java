package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.EquipmentDTO;
import swp490.greeenslot.service.EquipmentService;

import java.util.List;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/equipment")
@Tag(name = "Equipment Management", description = "APIs for managing equipment and tools")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get all equipment")
    public ResponseEntity<List<EquipmentDTO>> getAllEquipment() {
        return ResponseEntity.ok(equipmentService.getAllEquipment());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get equipment by ID")
    public ResponseEntity<EquipmentDTO> getEquipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getEquipmentById(id));
    }

    @GetMapping("/pillar/{pillarId}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get equipment by pillar")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentByPillar(@PathVariable Long pillarId) {
        return ResponseEntity.ok(equipmentService.getEquipmentByPillar(pillarId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get equipment by status")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentByStatus(@PathVariable String status) {
        return ResponseEntity.ok(equipmentService.getEquipmentByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create new equipment")
    public ResponseEntity<EquipmentDTO> createEquipment(@Valid @RequestBody EquipmentDTO dto) {
        return ResponseEntity.ok(equipmentService.createEquipment(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update equipment")
    public ResponseEntity<EquipmentDTO> updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentDTO dto) {
        return ResponseEntity.ok(equipmentService.updateEquipment(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete equipment")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }
}
