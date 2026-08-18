package swp490.greeenslot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.PumpStatusDTO;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class PumpService {

    private static final Logger logger = LoggerFactory.getLogger(PumpService.class);

    // Lưu trạng thái mặc định ban đầu là tắt
    private volatile String currentStatus = "OFF";

    // Chế độ tự động xịt/tưới nước khi độ ẩm đất thấp (mặc định bật)
    private volatile boolean autoMode = true;

    // Lưu thông tin lần kích hoạt gần nhất
    private volatile String lastTriggerReason = "Hệ thống sẵn sàng";
    private volatile LocalDateTime lastTriggerTime = null;

    // Thời gian tối thiểu giữa 2 lần tự động kích hoạt bơm (cooldown 10s để hệ thống phản hồi nhanh khi test)
    private static final long AUTO_TRIGGER_COOLDOWN_SECONDS = 10;

    public String getPumpStatus() {
        return currentStatus;
    }

    public boolean isAutoMode() {
        return autoMode;
    }

    public void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
        logger.info("Pump auto mode changed to: {}", autoMode);
    }

    public void setPumpStatus(String status) {
        if (status != null && (status.equalsIgnoreCase("ON") || status.equalsIgnoreCase("OFF"))) {
            this.currentStatus = status.toUpperCase();
            this.lastTriggerTime = LocalDateTime.now();
            this.lastTriggerReason = "Điều khiển thủ công (" + this.currentStatus + ")";
            logger.info("Pump status manually updated to: {}", this.currentStatus);
        }
    }

    /**
     * Tự động kích hoạt máy bơm khi cảm biến ghi nhận độ ẩm đất thấp hoặc ánh sáng gắt vượt ngưỡng.
     * @param reason Lý do kích hoạt
     * @return true nếu kích hoạt thành công, false nếu đang bị cooldown hoặc tắt autoMode
     */
    public synchronized boolean triggerAutoSpray(String reason) {
        if (!autoMode) {
            logger.debug("Auto pump spray skipped: Auto mode is disabled.");
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if ("ON".equalsIgnoreCase(currentStatus)) {
            // Máy bơm đang bật
            return false;
        }

        if (lastTriggerTime != null) {
            long secondsSinceLastTrigger = ChronoUnit.SECONDS.between(lastTriggerTime, now);
            if (secondsSinceLastTrigger < AUTO_TRIGGER_COOLDOWN_SECONDS) {
                logger.debug("Auto pump spray skipped: Cooldown active ({}s / {}s).", secondsSinceLastTrigger, AUTO_TRIGGER_COOLDOWN_SECONDS);
                return false;
            }
        }

        this.currentStatus = "ON";
        this.lastTriggerTime = now;
        this.lastTriggerReason = reason;
        logger.warn("💧 [AUTO-SPRAY ACTIVATED] Kích hoạt máy bơm tự động: {}", reason);
        return true;
    }

    public PumpStatusDTO getFullStatus() {
        return PumpStatusDTO.builder()
                .status(currentStatus)
                .autoMode(autoMode)
                .lastTriggerReason(lastTriggerReason)
                .lastTriggerTime(lastTriggerTime)
                .build();
    }
}