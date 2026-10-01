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
        recordHarvest(rental, method, staff, null, null, null);
    }

    @Override
    public void recordHarvest(SlotRental rental, String method, User staff, String pillarCodes) {
        recordHarvest(rental, method, staff, pillarCodes, null, null);
    }

    @Override
    public void recordHarvest(SlotRental rental, String method, User staff, String pillarCodes, String evidenceImageUrl, String staffNotes) {
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
            history.setEvidenceImageUrl(evidenceImageUrl);
            history.setStaffNotes(staffNotes);

            harvestHistoryRepository.save(history);
        }
    }

    @jakarta.annotation.PostConstruct
    public void autoMigrateAndSplitCommaSeparatedHistories() {
        try {
            List<HarvestHistory> all = harvestHistoryRepository.findAll();
            for (HarvestHistory h : all) {
                String raw = h.getPillarCodes();
                if (raw != null && raw.contains(",")) {
                    List<String> codes = java.util.Arrays.stream(raw.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .collect(java.util.stream.Collectors.toList());
                    if (codes.size() > 1) {
                        h.setPillarCodes(codes.get(0));
                        harvestHistoryRepository.save(h);

                        for (int i = 1; i < codes.size(); i++) {
                            HarvestHistory extra = cloneHistory(h, codes.get(i));
                            harvestHistoryRepository.save(extra);
                        }
                    } else if (codes.size() == 1) {
                        h.setPillarCodes(codes.get(0));
                        harvestHistoryRepository.save(h);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore any startup migration exceptions
        }
    }

    private HarvestHistory cloneHistory(HarvestHistory original, String singlePillarCode) {
        HarvestHistory copy = new HarvestHistory();
        copy.setRentalId(original.getRentalId());
        copy.setLocationId(original.getLocationId());
        copy.setLocationName(original.getLocationName());
        copy.setSlotId(original.getSlotId());
        copy.setSlotNumber(original.getSlotNumber());
        copy.setTreeId(original.getTreeId());
        copy.setTreeName(original.getTreeName());
        copy.setCustomerId(original.getCustomerId());
        copy.setCustomerName(original.getCustomerName());
        copy.setHarvestMethod(original.getHarvestMethod());
        copy.setStaffId(original.getStaffId());
        copy.setStaffName(original.getStaffName());
        copy.setPlantedAt(original.getPlantedAt());
        copy.setHarvestedAt(original.getHarvestedAt());
        copy.setPillarCodes(singlePillarCode);
        copy.setHarvestDays(original.getHarvestDays());
        copy.setDaysGrown(original.getDaysGrown());
        copy.setIsEarlyHarvest(original.getIsEarlyHarvest());
        copy.setPillarHarvestCount(original.getPillarHarvestCount());
        copy.setEvidenceImageUrl(original.getEvidenceImageUrl());
        copy.setStaffNotes(original.getStaffNotes());
        return copy;
    }

    private List<HarvestHistory> splitAndNormalizeHistory(List<HarvestHistory> list) {
        if (list == null || list.isEmpty()) return new java.util.ArrayList<>();
        List<HarvestHistory> result = new java.util.ArrayList<>();
        for (HarvestHistory h : list) {
            String rawCodes = h.getPillarCodes();
            if (rawCodes != null && rawCodes.contains(",")) {
                List<String> codes = java.util.Arrays.stream(rawCodes.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());
                for (String code : codes) {
                    HarvestHistory copy = cloneHistory(h, code);
                    result.add(copy);
                }
            } else {
                result.add(h);
            }
        }
        return result;
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
                h.setPillarHarvestCount(i + 1);
            }
        }
        // Sắp xếp lại danh sách: mới nhất lên đầu
        list.sort((a, b) -> {
            LocalDateTime tA = a.getHarvestedAt() != null ? a.getHarvestedAt() : a.getPlantedAt();
            LocalDateTime tB = b.getHarvestedAt() != null ? b.getHarvestedAt() : b.getPlantedAt();
            if (tA != null && tB != null) {
                int cmp = tB.compareTo(tA);
                if (cmp != 0) return cmp;
            } else if (tA == null && tB != null) {
                return 1;
            } else if (tA != null && tB == null) {
                return -1;
            }
            Long idA = a.getId() != null ? a.getId() : 0L;
            Long idB = b.getId() != null ? b.getId() : 0L;
            return idB.compareTo(idA);
        });
    }

    @Override
    public List<HarvestHistory> getMyHistory(String username) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        List<HarvestHistory> history = harvestHistoryRepository.findByCustomerIdOrderByHarvestedAtDesc(user.getId());
        List<HarvestHistory> normalized = splitAndNormalizeHistory(history);
        enrichHarvestCounts(normalized);
        return normalized;
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
        List<HarvestHistory> normalized = splitAndNormalizeHistory(history);
        enrichHarvestCounts(normalized);
        return normalized;
    }
}
