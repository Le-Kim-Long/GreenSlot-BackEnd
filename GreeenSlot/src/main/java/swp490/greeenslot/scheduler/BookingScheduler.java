package swp490.greeenslot.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.*;

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
        }
    }

    /**
     * Runs daily at midnight.
     * Finds all ACTIVE rentals where endTime is in the past,
     * updates them to EXPIRED, and releases the GardenSlot back to AVAILABLE
     * if there are no other active or pending rentals.
     */
    @Scheduled(cron = "0 0 0 * * *")
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
            logger.info("Expiring rental with ID: " + rental.getId());
            rental.setStatus(ERentalStatus.EXPIRED);
            slotRentalRepository.save(rental);

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
        }
    }
}
