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
        if (rental == null || rental.getTree() == null) {
            return;
        }

        GardenSlot slot = rental.getGardenSlot();
        Pillar pillar = slot != null ? slot.getPillar() : null;
        Location location = pillar != null ? pillar.getLocation() : null;

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
        history.setPlantedAt(rental.getPlantedAt());
        history.setHarvestedAt(LocalDateTime.now());

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
