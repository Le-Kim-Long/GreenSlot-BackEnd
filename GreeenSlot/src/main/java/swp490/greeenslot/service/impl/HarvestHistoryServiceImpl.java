package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp490.greeenslot.entity.GardenSlot;
import swp490.greeenslot.entity.HarvestHistory;
import swp490.greeenslot.entity.Location;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.HarvestHistoryRepository;
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.HarvestHistoryService;
import swp490.greeenslot.service.LocationContextService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
        if (rental == null) {
            return;
        }

        Tree tree = rental.getTree();
        if (tree == null && rental.getRentedPillars() != null && !rental.getRentedPillars().isEmpty()) {
            tree = rental.getRentedPillars().stream()
                    .map(Pillar::getDefaultTree)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        GardenSlot slot = rental.getGardenSlot();
        if (tree == null && slot != null && slot.getPillars() != null && !slot.getPillars().isEmpty()) {
            tree = slot.getPillars().stream()
                    .map(Pillar::getDefaultTree)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        Pillar pillar = slot != null ? slot.getPillar() : null;
        Location location = pillar != null ? pillar.getLocation() : null;
        if (location == null && slot != null && slot.getLocation() != null) {
            location = slot.getLocation();
        }

        // Determine pillar codes if not provided
        String finalPillarCodes = pillarCodes;
        if ((finalPillarCodes == null || finalPillarCodes.isBlank()) && rental.getHarvestPillarCode() != null && !rental.getHarvestPillarCode().isBlank()) {
            finalPillarCodes = rental.getHarvestPillarCode();
        }
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
        
        Integer harvestDays = (tree != null && tree.getHarvestDays() != null) ? tree.getHarvestDays() : 30;
        int daysGrown = 0;
        if (plantedAt != null) {
            daysGrown = (int) Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(plantedAt, now));
        }
        boolean isEarly = daysGrown < harvestDays;

        List<String> individualPillars = java.util.Arrays.stream((finalPillarCodes != null ? finalPillarCodes : "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (individualPillars.isEmpty()) {
            individualPillars = List.of(finalPillarCodes != null && !finalPillarCodes.isBlank() ? finalPillarCodes.trim() : "Trụ mặc định");
        }

        for (String singlePillarCode : individualPillars) {
            long existingCount = harvestHistoryRepository.countHarvestsForPillar(rental.getId(), singlePillarCode);
            int currentHarvestCount = (int) existingCount + 1;

            HarvestHistory history = new HarvestHistory();
            history.setRentalId(rental.getId());
            history.setLocationId(location != null ? location.getId() : null);
            history.setLocationName(location != null ? location.getName() : null);
            history.setSlotId(slot != null ? slot.getId() : null);
            history.setSlotNumber(slot != null ? slot.getSlotNumber() : null);
            history.setTreeId(tree != null ? tree.getId() : null);
            history.setTreeName(tree != null ? tree.getTreeName() : "Rau/Cây trồng");
            history.setCustomerId(rental.getUser() != null ? rental.getUser().getId() : null);
            history.setCustomerName(rental.getUser() != null ? rental.getUser().getFullName() : null);
            history.setHarvestMethod(method);
            history.setStaffId(staff != null ? staff.getId() : null);
            history.setStaffName(staff != null ? staff.getFullName() : null);
            history.setPlantedAt(plantedAt);
            history.setHarvestedAt(now);
            history.setPillarCodes(singlePillarCode);
            history.setPillarHarvestCount(currentHarvestCount);
            history.setHarvestDays(harvestDays);
            history.setDaysGrown(daysGrown);
            history.setIsEarlyHarvest(isEarly);

            harvestHistoryRepository.save(history);
        }
    }

    private void enrichHarvestCounts(List<HarvestHistory> list) {
        if (list == null || list.isEmpty()) return;
        java.util.Map<String, java.util.List<HarvestHistory>> grouped = new java.util.HashMap<>();
        for (HarvestHistory h : list) {
            String key = (h.getRentalId() != null ? h.getRentalId() : 0L) + "_" + (h.getPillarCodes() != null ? h.getPillarCodes().trim() : "ALL");
            grouped.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(h);
        }
        for (java.util.List<HarvestHistory> group : grouped.values()) {
            group.sort(java.util.Comparator.comparing(HarvestHistory::getHarvestedAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
            for (int i = 0; i < group.size(); i++) {
                HarvestHistory h = group.get(i);
                if (h.getPillarHarvestCount() == null || h.getPillarHarvestCount() <= 0) {
                    h.setPillarHarvestCount(i + 1);
                }
            }
        }
    }

    @Override
    public List<HarvestHistory> getMyHistory(String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        List<HarvestHistory> history = harvestHistoryRepository.findByCustomerIdOrderByHarvestedAtDesc(user.getId());
        enrichHarvestCounts(history);
        return history;
    }

    @Override
    public List<HarvestHistory> getHistoryForManager(String username) {
        Long targetLocationId = locationContextService.resolveTargetLocationId(null);
        List<HarvestHistory> history;
        if (targetLocationId == null) {
            history = harvestHistoryRepository.findAllByOrderByHarvestedAtDesc();
        } else {
            history = harvestHistoryRepository.findByLocationIdOrderByHarvestedAtDesc(targetLocationId);
        }
        enrichHarvestCounts(history);
        return history;
    }
}
