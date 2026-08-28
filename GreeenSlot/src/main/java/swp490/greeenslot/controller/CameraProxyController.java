package swp490.greeenslot.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import swp490.greeenslot.dto.CameraPingRequestDTO;
import java.util.Collection;

@RestController
@RequestMapping("/api/iot/camera")
@CrossOrigin(origins = "*")
public class CameraProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    // Tiêm (Inject) CameraController vào để lấy danh sách IP động từ mạch ESP32
    @Autowired
    private CameraController cameraController;

    @GetMapping(value = "/snapshot", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getSnapshot() {
        try {
            // 1. Lấy danh sách các camera đang online từ bộ nhớ RAM
            Collection<CameraPingRequestDTO> activeCameras = cameraController.getCameraRegistry().values();

            // Nếu chưa có camera nào ping lên, báo lỗi 503 ngay lập tức
            if (activeCameras.isEmpty()) {
                System.err.println("[PROXY ERROR] Không có Camera nào đang kết nối. Vui lòng kiểm tra mạch ESP32.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }

            // 2. Lấy thông tin camera đầu tiên trong danh sách (Dùng cho giao diện Camera Tổng)
            CameraPingRequestDTO camInfo = activeCameras.iterator().next();
            String dynamicCaptureUrl = camInfo.getCaptureUrl(); // Sẽ tự động là http://10.10.x.x/capture

            // 3. Dùng RestTemplate gọi thẳng vào IP động vừa lấy được
            byte[] imageBytes = restTemplate.getForObject(dynamicCaptureUrl, byte[].class);

            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().getHeaderValue());

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("[PROXY ERROR] Mất kết nối tới Camera: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}