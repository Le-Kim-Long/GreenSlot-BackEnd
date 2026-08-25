package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.HarvestHistory;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.HarvestHistoryRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.HarvestHistoryService;
import swp490.greeenslot.service.LocationContextService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HarvestHistoryServiceImpl implements HarvestHistoryService {

    @Autowired
    private HarvestHistoryRepository harvestHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationContextService locationContextService;

    @Override
    public void recordHarvest(SlotRental rental, String method, User staff) {
        recordHarvest(rental, method, staff, null);
    }

    @Override
    public void recordHarvest(SlotRental rental, String method, User staff, String pillarCodes) {
        if (rental == null || rental.getTree() == null) {
            return;
        }

        GardenSlot slot = rental.getGardenSlot();
        Pillar pillar = slot != null ? slot.getPillar() : null;
        Location location = pillar != null ? pillar.getLocation() : null;
        if (location == null && slot != null && slot.getLocation() != null) {
            location = slot.getLocation();
        }

        // Determine pillar codes if not provided
        String finalPillarCodes = pillarCodes;
        if (finalPillarCodes == null || finalPillarCodes.isBlank()) {
            if (rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()) {
                finalPillarCodes = rental.getRentedPillars().stream()
                        .map(Pillar::getPillarCode)
                        .filter(code -> code != null && !code.isBlank())
                        .collect(java.util.stream.Collectors.joining(", "));
            } else if (pillar != null && pillar.getPillarCode() != null) {
                finalPillarCodes = pillar.getPillarCode();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime plantedAt = rental.getPlantedAt() != null ? rental.getPlantedAt() : rental.getStartTime();
        
        Integer harvestDays = rental.getTree().getHarvestDays() != null ? rental.getTree().getHarvestDays() : 30;
        int daysGrown = 0;
        if (plantedAt != null) {
            daysGrown = (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(plantedAt, now));
        }
        boolean isEarly = daysGrown < harvestDays;

        HarvestHistory history = new HarvestHistory();
        history.setRentalId(rental.getId());
        history.setLocationId(location != null ? location.getId() : null);
        history.setLocationName(location != null ? location.getName() : null);
        history.setSlotId(slot != null ? slot.getId() : null);
        history.setSlotNumber(slot != null ? slot.getSlotNumber() : null);
        history.setTreeId(rental.getTree().getId());
        history.setTreeName(rental.getTree().getTreeName());
        history.setCustomerId(rental.getUser() != null ? rental.getUser().getId() : null);
        history.setCustomerName(rental.getUser() != null ? rental.getUser().getFullName() : null);
        history.setHarvestMethod(method);
        history.setStaffId(staff != null ? staff.getId() : null);
        history.setStaffName(staff != null ? staff.getFullName() : null);
        history.setPlantedAt(plantedAt);
        history.setHarvestedAt(now);
        history.setPillarCodes(finalPillarCodes);
        history.setHarvestDays(harvestDays);
        history.setDaysGrown(daysGrown);
        history.setIsEarlyHarvest(isEarly);

        harvestHistoryRepository.save(history);
    }

    @Override
    public List<HarvestHistory> getMyHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return harvestHistoryRepository.findByCustomerIdOrderByHarvestedAtDesc(user.getId());
    }

    @Override
    public List<HarvestHistory> getHistoryForManager(String username) {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        if (targetLocationId == null) {
            return harvestHistoryRepository.findAllByOrderByHarvestedAtDesc();
        }
        return harvestHistoryRepository.findByLocationIdOrderByHarvestedAtDesc(targetLocationId);
    }
}
