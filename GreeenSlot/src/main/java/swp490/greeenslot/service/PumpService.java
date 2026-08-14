package swp490.greeenslot.service;

import org.springframework.stereotype.Service;

@Service
public class PumpService {

    // Lưu trạng thái mặc định ban đầu là tắt
    private String currentStatus = "OFF";

    public String getPumpStatus() {
        return currentStatus;
    }

    public void setPumpStatus(String status) {
        // Chỉ chấp nhận 2 giá trị ON hoặc OFF để tránh lỗi
        if (status != null && (status.equalsIgnoreCase("ON") || status.equalsIgnoreCase("OFF"))) {
            this.currentStatus = status.toUpperCase();
        }
    }
}