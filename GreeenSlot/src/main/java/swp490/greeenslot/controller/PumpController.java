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
        String currentStatus = pumpService.getPumpStatus();
        return ResponseEntity.ok(new PumpStatusDTO(currentStatus));
    }

    @PostMapping("/status")
    @PreAuthorize("hasRole('ROLE_LOCATION_MANAGER') or hasRole('ROLE_ADMIN') or hasRole('ROLE_GARDEN_STAFF')")
    @Operation(summary = "Bật/Tắt máy bơm thủ công", description = "Truyền vào ON hoặc OFF để điều khiển máy bơm.")
    public ResponseEntity<PumpStatusDTO> updatePumpStatus(@RequestBody PumpStatusDTO requestDto) {
        // Cập nhật trạng thái vào Service
        pumpService.setPumpStatus(requestDto.getStatus());

        // Trả về trạng thái mới nhất để Client xác nhận
        String updatedStatus = pumpService.getPumpStatus();
        return ResponseEntity.ok(new PumpStatusDTO(updatedStatus));
    }
}