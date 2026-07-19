package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.TreeDTO;
import swp490.greeenslot.service.TreeService;

import java.util.List;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/trees")
@Tag(name = "Tree Management", description = "APIs for managing tree types, configurations, and thresholds")
public class TreeController {

    @Autowired
    private TreeService treeService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get all trees")
    public ResponseEntity<List<TreeDTO>> getAllTrees() {
        return ResponseEntity.ok(treeService.getAllTrees());
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get active trees")
    public ResponseEntity<List<TreeDTO>> getActiveTrees() {
        return ResponseEntity.ok(treeService.getActiveTrees());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Get tree by ID")
    public ResponseEntity<TreeDTO> getTreeById(@PathVariable Long id) {
        return ResponseEntity.ok(treeService.getTreeById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Create a new tree")
    public ResponseEntity<TreeDTO> createTree(@Valid @RequestBody TreeDTO dto) {
        return ResponseEntity.ok(treeService.createTree(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Update an existing tree")
    public ResponseEntity<TreeDTO> updateTree(@PathVariable Long id, @Valid @RequestBody TreeDTO dto) {
        return ResponseEntity.ok(treeService.updateTree(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_MANAGER')")
    @Operation(summary = "Delete a tree (soft delete)")
    public ResponseEntity<Void> deleteTree(@PathVariable Long id) {
        treeService.deleteTree(id);
        return ResponseEntity.noContent().build();
    }
}
