package swp490.greeenslot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import swp490.greeenslot.dto.PumpStatusDTO;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PumpService {

    private static final Logger logger = LoggerFactory.getLogger(PumpService.class);

    // Lưu trạng thái mặc định ban đầu là tắt (toàn cục)
    private volatile String currentStatus = "OFF";

    // Chế độ tự động xịt/tưới nước khi độ ẩm đất thấp (mặc định bật)
    private volatile boolean autoMode = true;

    // Lưu thông tin lần kích hoạt gần nhất
    private volatile String lastTriggerReason = "Hệ thống sẵn sàng";
    private volatile LocalDateTime lastTriggerTime = null;

    // Thời gian tối thiểu giữa 2 lần tự động kích hoạt bơm (cooldown 10s để hệ thống phản hồi nhanh khi test)
    private static final long AUTO_TRIGGER_COOLDOWN_SECONDS = 10;

    // Quản lý trạng thái máy bơm riêng cho từng Trụ (1 trụ = 1 máy bơm)
    private final ConcurrentHashMap<Long, PumpStatusDTO> pillarStatuses = new ConcurrentHashMap<>();
    private final ScheduledExecutorService autoOffScheduler = Executors.newScheduledThreadPool(2);

    // ==========================================
    // CÁC HÀM QUẢN LÝ MÁY BƠM THEO TỪNG TRỤ
    // ==========================================

    public PumpStatusDTO getPillarPumpStatus(Long pillarId) {
        if (pillarId == null) {
            return getFullStatus();
        }
        return pillarStatuses.computeIfAbsent(pillarId, id -> PumpStatusDTO.builder()
                .status("OFF")
                .autoMode(true)
                .lastTriggerReason("Hệ thống sẵn sàng")
                .lastTriggerTime(null)
                .build());
    }

    public PumpStatusDTO setPillarPumpStatus(Long pillarId, String status, int autoOffSeconds) {
        if (pillarId == null) {
            setPumpStatus(status);
            return getFullStatus();
        }
        String cleanStatus = (status != null && status.equalsIgnoreCase("ON")) ? "ON" : "OFF";
        LocalDateTime now = LocalDateTime.now();
        String reason = cleanStatus.equals("ON") ? "Kích hoạt thủ công (" + autoOffSeconds + "s)" : "Tắt thủ công";

        PumpStatusDTO dto = pillarStatuses.compute(pillarId, (id, current) -> {
            boolean auto = (current != null && current.getAutoMode() != null) ? current.getAutoMode() : true;
            return PumpStatusDTO.builder()
                    .status(cleanStatus)
                    .autoMode(auto)
                    .lastTriggerReason(reason)
                    .lastTriggerTime(now)
                    .build();
        });

        logger.info("💧 [PILLAR PUMP] Trụ ID {} chuyển trạng thái máy bơm thành: {} ({})", pillarId, cleanStatus, reason);

        // Nếu bật và có hẹn giờ tự ngắt (mặc định 5s)
        if ("ON".equalsIgnoreCase(cleanStatus) && autoOffSeconds > 0) {
            autoOffScheduler.schedule(() -> {
                try {
                    PumpStatusDTO cur = pillarStatuses.get(pillarId);
                    if (cur != null && "ON".equalsIgnoreCase(cur.getStatus())) {
                        pillarStatuses.put(pillarId, PumpStatusDTO.builder()
                                .status("OFF")
                                .autoMode(cur.getAutoMode())
                                .lastTriggerReason("Tự ngắt an toàn sau " + autoOffSeconds + "s")
                                .lastTriggerTime(LocalDateTime.now())
                                .build());
                        logger.info("🛑 [PILLAR PUMP AUTO-OFF] Máy bơm trụ ID {} đã tự ngắt an toàn sau {}s.", pillarId, autoOffSeconds);
                    }
                } catch (Exception e) {
                    logger.error("Lỗi khi tự ngắt máy bơm trụ ID {}: {}", pillarId, e.getMessage());
                }
            }, autoOffSeconds, TimeUnit.SECONDS);
        }

        return dto;
    }

    public PumpStatusDTO setPillarAutoMode(Long pillarId, boolean autoMode) {
        if (pillarId == null) {
            setAutoMode(autoMode);
            return getFullStatus();
        }
        return pillarStatuses.compute(pillarId, (id, current) -> {
            String status = (current != null && current.getStatus() != null) ? current.getStatus() : "OFF";
            String reason = (current != null) ? current.getLastTriggerReason() : "Hệ thống sẵn sàng";
            LocalDateTime time = (current != null) ? current.getLastTriggerTime() : null;
            return PumpStatusDTO.builder()
                    .status(status)
                    .autoMode(autoMode)
                    .lastTriggerReason(reason)
                    .lastTriggerTime(time)
                    .build();
        });
    }

    // ==========================================
    // CÁC HÀM TOÀN CỤC (TƯƠNG THÍCH NGƯỢC)
    // ==========================================

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

            if ("ON".equalsIgnoreCase(this.currentStatus)) {
                autoOffScheduler.schedule(() -> {
                    if ("ON".equalsIgnoreCase(this.currentStatus)) {
                        this.currentStatus = "OFF";
                        this.lastTriggerReason = "Tự ngắt an toàn sau 5s";
                        this.lastTriggerTime = LocalDateTime.now();
                        logger.info("🛑 [GLOBAL PUMP AUTO-OFF] Máy bơm toàn cục đã tự ngắt an toàn sau 5s.");
                    }
                }, 5, TimeUnit.SECONDS);
            }
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

        autoOffScheduler.schedule(() -> {
            if ("ON".equalsIgnoreCase(this.currentStatus)) {
                this.currentStatus = "OFF";
                this.lastTriggerReason = "Tự ngắt an toàn sau 5s";
                this.lastTriggerTime = LocalDateTime.now();
            }
        }, 5, TimeUnit.SECONDS);

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