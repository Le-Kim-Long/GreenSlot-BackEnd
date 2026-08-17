package swp490.greeenslot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.ETaskStatus;
import swp490.greeenslot.entity.ETaskType;
import swp490.greeenslot.entity.GardeningTask;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.service.HarvestReminderService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HarvestReminderServiceImpl implements HarvestReminderService {

    private static final Logger logger = LoggerFactory.getLogger(HarvestReminderServiceImpl.class);

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Override
    @Scheduled(cron = "0 0 8 * * ?") // Run daily at 8 AM
    @Transactional
    public void checkAndNotifyHarvestReady() {
        LocalDateTime now = LocalDateTime.now();
        List<SlotRental> candidates = slotRentalRepository.findHarvestReminderCandidates();

        logger.info("Checking {} rentals for harvest readiness", candidates.size());

        for (SlotRental rental : candidates) {
            try {
                if (Boolean.TRUE.equals(rental.getHarvestReminderSent())) {
                    continue;
                }

                Tree tree = rental.getTree();
                Integer harvestDays = tree != null ? tree.getHarvestDays() : null;
                if (harvestDays == null || harvestDays <= 0 || rental.getPlantedAt() == null) {
                    continue;
                }

                LocalDateTime harvestReadyAt = rental.getPlantedAt().plusDays(harvestDays);
                if (harvestReadyAt.isAfter(now)) {
                    continue; // chưa tới ngày thu hoạch
                }

                String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";

                // Chỉ tạo công việc thu hoạch cho nhân viên làm vườn xử lý.
                // Khách hàng CHƯA được báo ở bước này — chỉ được báo khi staff hoàn tất
                // và quản lý duyệt (xem GardeningTaskServiceImpl.reviewTaskEvidence).
                GardeningTask task = new GardeningTask();
                task.setTaskName("Thu hoach: " + tree.getTreeName() + " - O " + slotNumber);
                task.setDescription("Cay " + tree.getTreeName() + " tai o dat " + slotNumber
                        + " da du " + harvestDays + " ngay sinh truong, can thu hoach.");
                task.setStatus(ETaskStatus.PENDING);
                task.setTaskType(ETaskType.HARVEST);
                task.setTargetSlot(rental.getGardenSlot());
                task.setRequestedBy(rental.getUser());
                task.setAssignedStaff(null); // Chờ quản lý/nhân viên nhận việc
                gardeningTaskRepository.save(task);

                // 3. Đánh dấu đã nhắc để không lặp lại mỗi ngày
                rental.setHarvestReminderSent(true);
                slotRentalRepository.save(rental);

                logger.info("Sent harvest-ready reminder for rental ID {}", rental.getId());
            } catch (Exception e) {
                logger.error("Failed to send harvest reminder for rental ID {}: {}", rental.getId(), e.getMessage());
            }
        }
    }
}
