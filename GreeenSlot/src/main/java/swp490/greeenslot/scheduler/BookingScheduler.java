package swp490.greeenslot.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;
import swp490.greeenslot.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Component
public class BookingScheduler {

    private static final Logger logger = Logger.getLogger(BookingScheduler.class.getName());

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private GardenSlotRepository gardenSlotRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    /**
     * Runs every 15 minutes.
     * Finds all SlotRentals stuck in PENDING status for more than 30 minutes,
     * updates them to CANCELLED, updates associated pending transactions to EXPIRED,
     * and releases the GardenSlot back to AVAILABLE if there are no other active or pending rentals.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void cleanUpStalePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        logger.info("Running cleanUpStalePendingBookings scheduler with cutoff: " + cutoff);

        List<SlotRental> staleRentals = slotRentalRepository.findStalePendingRentals(cutoff);
        if (staleRentals.isEmpty()) {
            logger.info("No stale pending bookings found.");
            return;
        }

        for (SlotRental rental : staleRentals) {
            try {
                logger.info("Cleaning up stale pending rental with ID: " + rental.getId());
                rental.setStatus(ERentalStatus.CANCELLED);
                slotRentalRepository.save(rental);

                // Update associated PENDING transactions to EXPIRED
                List<PaymentTransaction> txns = paymentTransactionRepository.findByRentalIdOrderByPaymentDateDesc(rental.getId());
                for (PaymentTransaction txn : txns) {
                    if (txn.getStatus() == EPaymentStatus.PENDING) {
                        txn.setStatus(EPaymentStatus.EXPIRED);
                        paymentTransactionRepository.save(txn);
                    }
                }

                List<GardeningTask> pendingTasks = gardeningTaskRepository.findPendingTasksBySlotId(rental.getGardenSlot().getId());
                for (GardeningTask task : pendingTasks) {
                    task.setStatus(ETaskStatus.CANCELLED);
                    gardeningTaskRepository.save(task);
                }

                // Release slot only if no other active or pending rentals exist
                GardenSlot slot = rental.getGardenSlot();
                long otherCount = slotRentalRepository.countOtherActiveOrPending(slot.getId(), rental.getId());
                if (otherCount == 0) {
                    slot.setStatus(ESlotStatus.AVAILABLE);
                    gardenSlotRepository.save(slot);
                    logger.info("Released slot " + slot.getId() + " back to AVAILABLE.");
                } else {
                    logger.info("Slot " + slot.getId() + " remains reserved due to other active or pending rentals.");
                }
            } catch (Exception e) {
                logger.warning("Failed to clean up stale rental ID " + rental.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Runs every 5 minutes.
     * Finds all ACTIVE rentals where endTime is in the past,
     * updates them to EXPIRED, and releases the GardenSlot back to AVAILABLE
     * if there are no other active or pending rentals.
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expireFinishedRentals() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Running expireFinishedRentals scheduler at: " + now);

        List<SlotRental> expiredRentals = slotRentalRepository.findExpiredRentals(now);
        if (expiredRentals.isEmpty()) {
            logger.info("No expired rentals found.");
            return;
        }

        for (SlotRental rental : expiredRentals) {
            try {
                logger.info("Expiring rental with ID: " + rental.getId());
                rental.setStatus(ERentalStatus.EXPIRED);
                slotRentalRepository.save(rental);

                // Send notification to customer
                if (rental.getUser() != null && notificationService != null) {
                    String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";
                    String title = "Hợp đồng thuê đã hết hạn";
                    String message = String.format("Hợp đồng thuê ô đất %s đã hết hạn vào %s. Ô đất đã được hệ thống tự động thu hồi.",
                            slotNumber, rental.getEndTime());
                    notificationService.createNotification(
                            rental.getUser().getId(),
                            title,
                            message,
                            "RENTAL_EXPIRED",
                            rental.getId(),
                            "/dashboard/customer/rentals"
                    );
                }

                // Release slot only if no other active or pending rentals exist
                GardenSlot slot = rental.getGardenSlot();
                if (slot != null) {
                    long otherCount = slotRentalRepository.countOtherActiveOrPending(slot.getId(), rental.getId());
                    if (otherCount == 0) {
                        slot.setStatus(ESlotStatus.AVAILABLE);
                        gardenSlotRepository.save(slot);
                        logger.info("Released slot " + slot.getId() + " back to AVAILABLE.");
                    } else {
                        logger.info("Slot " + slot.getId() + " remains reserved due to other active or pending rentals.");
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to expire rental ID " + rental.getId() + ": " + e.getMessage());
            }
        }
    }
}
