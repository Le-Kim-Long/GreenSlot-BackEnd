package swp490.greeenslot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp490.greeenslot.dto.CameraPingRequestDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/cameras")
@CrossOrigin(origins = "*") // Cho phép FrontEnd gọi API không bị lỗi CORS
public class CameraController {

    // Bộ nhớ tạm lưu danh sách Camera trong RAM (Có thể thay bằng lưu vào Database JPA sau này)
    private final Map<String, CameraPingRequestDTO> cameraRegistry = new ConcurrentHashMap<>();

    // 1. API cho ESP32-CAM gọi lên để báo cáo IP: POST /api/cameras/ping
    @PostMapping("/ping")
    public ResponseEntity<swp490.greeenslot.dto.MessageResponseDTO> pingCamera(@RequestBody CameraPingRequestDTO request) {
        // Lưu thông tin Camera mới nhất vào danh sách
        cameraRegistry.put(request.getCamId(), request);
        System.out.println("[CAM UPDATE] Nhận tín hiệu từ: " + request.getCamId() + " -> IP: " + request.getIp());
        return ResponseEntity.ok().body(new swp490.greeenslot.dto.MessageResponseDTO("IP updated successfully!"));
    }

    // 2. API cho Web FrontEnd gọi để lấy danh sách Camera hiển thị: GET /api/cameras
    @GetMapping
    public ResponseEntity<java.util.Collection<CameraPingRequestDTO>> getAllCameras() {
        return ResponseEntity.ok(cameraRegistry.values());
    }
}