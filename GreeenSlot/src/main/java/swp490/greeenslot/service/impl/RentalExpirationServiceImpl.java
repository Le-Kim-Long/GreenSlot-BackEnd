package swp490.greeenslot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.ERentalStatus;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.service.FirebaseMessagingService;
import swp490.greeenslot.service.NotificationService;
import swp490.greeenslot.service.RentalExpirationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalExpirationServiceImpl implements RentalExpirationService {

    private static final Logger logger = LoggerFactory.getLogger(RentalExpirationServiceImpl.class);

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
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
                String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";
                String message = String.format("Your rental for slot %s will expire on %s. Please extend your rental to avoid service interruption.",
                        slotNumber, rental.getEndTime());

                notificationService.createNotification(
                        rental.getUser().getId(),
                        "Rental Expiring Soon",
                        message,
                        "RENTAL_EXPIRATION"
                );

                firebaseMessagingService.sendPushNotification(
                        rental.getUser().getId(),
                        "Rental Expiring Soon",
                        String.format("Slot %s expires on %s", slotNumber, rental.getEndTime())
                );

                logger.info("Sent expiration warning for rental ID {}", rental.getId());
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

                String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";
                String message = String.format("Your rental for slot %s has expired on %s. Please renew to continue using the service.",
                        slotNumber, rental.getEndTime());

                notificationService.createNotification(
                        rental.getUser().getId(),
                        "Rental Expired",
                        message,
                        "RENTAL_EXPIRED"
                );

                firebaseMessagingService.sendPushNotification(
                        rental.getUser().getId(),
                        "Rental Expired",
                        String.format("Slot %s rental expired. Please renew.", slotNumber)
                );

                logger.info("Processed expired rental ID {}", rental.getId());
            } catch (Exception e) {
                logger.error("Failed to process expired rental ID {}: {}", rental.getId(), e.getMessage());
            }
        }
    }
}
