package swp490.greeenslot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.ERentalStatus;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.repository.GardenSlotRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.service.FirebaseMessagingService;
import swp490.greeenslot.service.NotificationService;
import swp490.greeenslot.service.RentalExpirationService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalExpirationServiceImpl implements RentalExpirationService {

    private static final Logger logger = LoggerFactory.getLogger(RentalExpirationServiceImpl.class);

    @Autowired
    private SlotRentalRepository slotRentalRepository;
    
    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private swp490.greeenslot.repository.PillarRepository pillarRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired(required = false)
    private FirebaseMessagingService firebaseMessagingService;

    @Override
    @Scheduled(cron = "0 0 9 * * ?") // Run daily at 9 AM
    @Transactional
    public void checkAndNotifyExpiringRentals() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningThreshold = now.plusDays(7); // Notify 7 days before expiration

        List<SlotRental> activeRentals = slotRentalRepository.findAll().stream()
                .filter(r -> r.getStatus() == ERentalStatus.ACTIVE)
                .filter(r -> r.getEndTime() != null)
                .filter(r -> r.getEndTime().isBefore(warningThreshold) && r.getEndTime().isAfter(now))
                .toList();

        logger.info("Found {} rentals expiring within 7 days", activeRentals.size());

        for (SlotRental rental : activeRentals) {
            try {
                if (rental.getUser() == null) {
                    continue;
                }
                long hoursRemaining = Duration.between(now, rental.getEndTime()).toHours();
                long daysRemaining = Duration.between(now, rental.getEndTime()).toDays();
                String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";

                String notifType;
                String title;
                String message;

                if (hoursRemaining <= 24 && hoursRemaining > 0) {
                    notifType = "RENTAL_EXPIRING_1D";
                    title = "Hợp đồng thuê sắp hết hạn trong 24 giờ";
                    message = String.format("Hợp đồng thuê ô đất %s sẽ hết hạn vào %s (còn chưa đầy 24 giờ). Vui lòng gia hạn ngay để tránh gián đoạn dịch vụ.",
                            slotNumber, rental.getEndTime());
                } else if (daysRemaining <= 3 && daysRemaining > 1) {
                    notifType = "RENTAL_EXPIRING_3D";
                    title = "Hợp đồng thuê sắp hết hạn trong 3 ngày";
                    message = String.format("Hợp đồng thuê ô đất %s sẽ hết hạn vào %s (còn khoảng %d ngày). Vui lòng gia hạn để tiếp tục sử dụng.",
                            slotNumber, rental.getEndTime(), daysRemaining);
                } else {
                    notifType = "RENTAL_EXPIRING_7D";
                    title = "Hợp đồng thuê sắp hết hạn trong 7 ngày";
                    message = String.format("Hợp đồng thuê ô đất %s sẽ hết hạn vào %s (còn khoảng %d ngày). Hãy gia hạn để duy trì vườn cây.",
                            slotNumber, rental.getEndTime(), daysRemaining);
                }

                if (notificationService != null) {
                    notificationService.createNotification(
                            rental.getUser().getId(),
                            title,
                            message,
                            notifType,
                            rental.getId(),
                            "/dashboard/customer/rentals"
                    );
                }

                if (firebaseMessagingService != null) {
                    firebaseMessagingService.sendPushNotification(
                            rental.getUser().getId(),
                            title,
                            String.format("Ô %s sắp hết hạn vào %s", slotNumber, rental.getEndTime())
                    );
                }

                logger.info("Sent expiration warning ({}) for rental ID {}", notifType, rental.getId());
            } catch (Exception e) {
                logger.error("Failed to send expiration warning for rental ID {}: {}", rental.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Scheduled(cron = "0 0 10 * * ?") // Run daily at 10 AM
    @Transactional
    public void checkAndNotifyExpiredRentals() {
        LocalDateTime now = LocalDateTime.now();

        List<SlotRental> expiredRentals = slotRentalRepository.findAll().stream()
                .filter(r -> r.getStatus() == ERentalStatus.ACTIVE)
                .filter(r -> r.getEndTime() != null)
                .filter(r -> r.getEndTime().isBefore(now))
                .toList();

        logger.info("Found {} expired rentals to process", expiredRentals.size());

        for (SlotRental rental : expiredRentals) {
            try {
                rental.setStatus(ERentalStatus.EXPIRED);
                slotRentalRepository.save(rental);

                if (rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()) {
                    for (swp490.greeenslot.entity.Pillar p : rental.getRentedPillars()) {
                        p.setStatus(swp490.greeenslot.entity.EPillarStatus.ACTIVE);
                        pillarRepository.save(p);
                    }
                }
                
                if (rental.getGardenSlot() != null) {
                    swp490.greeenslot.entity.GardenSlot slot = rental.getGardenSlot();
                    slot.setStatus(swp490.greeenslot.entity.ESlotStatus.AVAILABLE);
                    gardenSlotRepository.save(slot);
                }

                String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";
                String title = "Hợp đồng thuê đã hết hạn";
                String message = String.format("Hợp đồng thuê ô đất %s đã hết hạn vào %s. Ô đất đã được hệ thống tự động thu hồi.",
                        slotNumber, rental.getEndTime());

                if (rental.getUser() != null && notificationService != null) {
                    notificationService.createNotification(
                            rental.getUser().getId(),
                            title,
                            message,
                            "RENTAL_EXPIRED",
                            rental.getId(),
                            "/dashboard/customer/rentals"
                    );
                }

                if (rental.getUser() != null && firebaseMessagingService != null) {
                    firebaseMessagingService.sendPushNotification(
                            rental.getUser().getId(),
                            title,
                            String.format("Ô %s đã hết hạn. Vui lòng đặt thuê mới nếu cần.", slotNumber)
                    );
                }

                logger.info("Processed expired rental ID {}", rental.getId());
            } catch (Exception e) {
                logger.error("Failed to process expired rental ID {}: {}", rental.getId(), e.getMessage());
            }
        }
    }
}
