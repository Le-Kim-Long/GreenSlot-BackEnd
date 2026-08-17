package swp490.greeenslot.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/iot/camera")
@CrossOrigin(origins = "*")
public class CameraProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    // Spring Boot sẽ tự lấy đường link từ file application.properties
    @Value("${spring.iot.camera.url}")
    private String esp32CamCaptureUrl;

    @GetMapping(value = "/snapshot", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getSnapshot() {
        try {
            // Nếu bạn bè pull code về mà cấu hình URL rỗng hoặc sai, nó sẽ nhảy xuống catch
            byte[] imageBytes = restTemplate.getForObject(esp32CamCaptureUrl, byte[].class);

            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noCache().getHeaderValue());

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Không kết nối được Camera: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}