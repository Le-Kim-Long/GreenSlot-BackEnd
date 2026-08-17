package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.PumpStatusDTO;
import swp490.greeenslot.service.PumpService;

@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@RestController
@RequestMapping("/api/iot/pump")
@Tag(name = "Pump Control API", description = "API điều khiển máy bơm tự động và thủ công")
public class PumpController {

    private final PumpService pumpService;

    // Dependency Injection
    public PumpController(PumpService pumpService) {
        this.pumpService = pumpService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_GARDEN_STAFF') or hasRole('ROLE_CUSTOMER')")
    @Operation(summary = "Lấy trạng thái máy bơm", description = "API này được Python Bridge và Frontend gọi để đồng bộ với mạch Arduino.")
    public ResponseEntity<PumpStatusDTO> getPumpStatus() {
        return ResponseEntity.ok(pumpService.getFullStatus());
    }

    @PostMapping("/status")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Bật/Tắt máy bơm hoặc cập nhật chế độ tự động", description = "Truyền vào ON hoặc OFF và cờ autoMode để điều khiển máy bơm.")
    public ResponseEntity<PumpStatusDTO> updatePumpStatus(@RequestBody PumpStatusDTO requestDto) {
        if (requestDto.getAutoMode() != null) {
            pumpService.setAutoMode(requestDto.getAutoMode());
        }
        if (requestDto.getStatus() != null && !requestDto.getStatus().isBlank()) {
            pumpService.setPumpStatus(requestDto.getStatus());
        }
        return ResponseEntity.ok(pumpService.getFullStatus());
    }

    @PutMapping("/auto-mode")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Bật/Tắt chế độ tự động tưới nước khi độ ẩm thấp")
    public ResponseEntity<PumpStatusDTO> setAutoMode(@RequestParam boolean enabled) {
        pumpService.setAutoMode(enabled);
        return ResponseEntity.ok(pumpService.getFullStatus());
    }
}