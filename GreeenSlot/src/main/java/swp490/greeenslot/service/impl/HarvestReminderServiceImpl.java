package swp490.greeenslot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.entity.*;
import swp490.greeenslot.repository.GardeningTaskRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.HarvestReminderService;
import swp490.greeenslot.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HarvestReminderServiceImpl implements HarvestReminderService {

    private static final Logger logger = LoggerFactory.getLogger(HarvestReminderServiceImpl.class);

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private GardeningTaskRepository gardeningTaskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

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

                String slotNumber = rental.getGardenSlot() != null ? rental.getGardenSlot().getSlotNumber() : "N/A";
                List<Pillar> rentedPillars = rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()
                        ? rental.getRentedPillars()
                        : (rental.getGardenSlot() != null && rental.getGardenSlot().getPillars() != null ? rental.getGardenSlot().getPillars() : List.of());

                boolean anyPillarHarvestTaskCreated = false;

                if (!rentedPillars.isEmpty()) {
                    for (Pillar p : rentedPillars) {
                        Tree tree = p.getDefaultTree() != null ? p.getDefaultTree() : rental.getTree();
                        Integer harvestDays = tree != null ? tree.getHarvestDays() : null;
                        if (harvestDays == null || harvestDays <= 0 || rental.getPlantedAt() == null) {
                            continue;
                        }

                        LocalDateTime harvestReadyAt = rental.getPlantedAt().plusDays(harvestDays);
                        if (harvestReadyAt.isAfter(now)) {
                            continue; // chưa tới ngày thu hoạch của trụ này
                        }

                        String pCode = p.getPillarCode() != null ? p.getPillarCode() : ("Trụ " + p.getId());
                        
                        // Kiểm tra xem đã có task thu hoạch cho trụ này chưa
                        List<GardeningTask> existingTasks = rental.getGardenSlot() != null
                                ? gardeningTaskRepository.findByTargetSlotIdAndTaskTypeOrderByCreatedAtDesc(rental.getGardenSlot().getId(), ETaskType.HARVEST)
                                : List.of();
                        boolean hasActive = existingTasks.stream().anyMatch(t -> 
                            t.getStatus() != ETaskStatus.COMPLETED && 
                            t.getStatus() != ETaskStatus.CANCELLED &&
                            ((t.getPillarCodes() != null && t.getPillarCodes().contains(pCode)) || t.getPillarCodes() == null)
                        );
                        if (hasActive) {
                            continue;
                        }

                        // Tạo công việc thu hoạch cho nhân viên làm vườn xử lý
                        GardeningTask task = new GardeningTask();
                        task.setTaskName("Thu hoạch: " + tree.getTreeName() + " - Ô " + slotNumber + " (Trụ " + pCode + ")");
                        task.setDescription("Cây " + tree.getTreeName() + " tại ô đất " + slotNumber + " (Trụ " + pCode + ")"
                                + " đã đủ " + harvestDays + " ngày sinh trưởng, cần thu hoạch.");
                        task.setStatus(ETaskStatus.PENDING);
                        task.setTaskType(ETaskType.HARVEST);
                        task.setTargetSlot(rental.getGardenSlot());
                        task.setRequestedBy(rental.getUser());
                        task.setAssignedStaff(null);
                        task.setPillarCodes(pCode);
                        task.setTreeName(tree.getTreeName());
                        task.setCreatedAt(now);
                        GardeningTask savedTask = gardeningTaskRepository.save(task);
                        anyPillarHarvestTaskCreated = true;

                        // Thông báo cho nhân viên tại địa điểm
                        notifyStaffAboutHarvest(rental, savedTask, tree.getTreeName(), slotNumber, pCode, harvestDays);
                    }
                } else if (rental.getTree() != null) {
                    Tree tree = rental.getTree();
                    Integer harvestDays = tree.getHarvestDays();
                    if (harvestDays != null && harvestDays > 0 && rental.getPlantedAt() != null) {
                        LocalDateTime harvestReadyAt = rental.getPlantedAt().plusDays(harvestDays);
                        if (!harvestReadyAt.isAfter(now)) {
                            GardeningTask task = new GardeningTask();
                            task.setTaskName("Thu hoạch: " + tree.getTreeName() + " - Ô " + slotNumber);
                            task.setDescription("Cây " + tree.getTreeName() + " tại ô đất " + slotNumber
                                    + " đã đủ " + harvestDays + " ngày sinh trưởng, cần thu hoạch.");
                            task.setStatus(ETaskStatus.PENDING);
                            task.setTaskType(ETaskType.HARVEST);
                            task.setTargetSlot(rental.getGardenSlot());
                            task.setRequestedBy(rental.getUser());
                            task.setAssignedStaff(null);
                            task.setTreeName(tree.getTreeName());
                            task.setCreatedAt(now);
                            GardeningTask savedTask = gardeningTaskRepository.save(task);
                            anyPillarHarvestTaskCreated = true;

                            notifyStaffAboutHarvest(rental, savedTask, tree.getTreeName(), slotNumber, "Toàn bộ trụ", harvestDays);
                        }
                    }
                }

                if (anyPillarHarvestTaskCreated) {
                    rental.setHarvestReminderSent(true);
                    slotRentalRepository.save(rental);
                    logger.info("Sent harvest-ready reminder for rental ID {}", rental.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to send harvest reminder for rental ID {}: {}", rental.getId(), e.getMessage());
            }
        }
    }

    private void notifyStaffAboutHarvest(SlotRental rental, GardeningTask savedTask, String treeName, String slotNumber, String pillarText, Integer harvestDays) {
        if (notificationService != null && rental.getGardenSlot() != null) {
            Long locationId = (rental.getGardenSlot().getLocation() != null)
                    ? rental.getGardenSlot().getLocation().getId()
                    : (rental.getGardenSlot().getPillar() != null && rental.getGardenSlot().getPillar().getLocation() != null
                        ? rental.getGardenSlot().getPillar().getLocation().getId() : null);
            List<User> staffList = locationId != null
                    ? userRepository.findByRoleNameAndLocation(ERole.ROLE_GARDEN_STAFF, locationId)
                    : List.of();
            if (staffList.isEmpty()) {
                staffList = userRepository.findByRoleName(ERole.ROLE_GARDEN_STAFF);
            }

            String title = "Cây đã đến ngày thu hoạch: Ô " + slotNumber + " (" + pillarText + ")";
            String message = String.format("Cây %s tại ô đất %s (%s) đã đến kỳ thu hoạch (%d ngày sinh trưởng). Vui lòng tiếp nhận nhiệm vụ.",
                    treeName, slotNumber, pillarText, harvestDays);

            for (User staff : staffList) {
                notificationService.createNotification(
                        staff.getId(),
                        title,
                        message,
                        "HARVEST_READY",
                        savedTask.getId(),
                        "/dashboard/garden-staff/schedules"
                );
            }
        }
    }
}
