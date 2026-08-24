package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.TreeDTO;
import swp490.greeenslot.entity.Alert;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.SlotRental;
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.entity.TreePlantingRequest;
import swp490.greeenslot.repository.AlertProcessingLogRepository;
import swp490.greeenslot.repository.AlertRepository;
import swp490.greeenslot.repository.PaymentTransactionRepository;
import swp490.greeenslot.repository.PillarRepository;
import swp490.greeenslot.repository.SlotRentalRepository;
import swp490.greeenslot.repository.TreePlantingRequestRepository;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.service.TreeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class TreeServiceImpl implements TreeService {

    @Autowired
    private TreeRepository treeRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AlertProcessingLogRepository alertProcessingLogRepository;

    @Autowired
    private PillarRepository pillarRepository;

    @Autowired
    private SlotRentalRepository slotRentalRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private TreePlantingRequestRepository treePlantingRequestRepository;

    @Override
    public List<TreeDTO> getAllTrees() {
        return treeRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TreeDTO getTreeById(Long id) {
        return treeRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Tree not found with id: " + id));
    }

    @Override
    @Transactional
    public TreeDTO createTree(TreeDTO dto) {
        validateTreeData(dto);
        Tree tree = mapToEntity(dto);
        tree.setIsActive(true);
        Tree savedTree = treeRepository.save(tree);
        return mapToDTO(savedTree);
    }

    @Override
    @Transactional
    public TreeDTO updateTree(Long id, TreeDTO dto) {
        validateTreeData(dto);
        Tree existingTree = treeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree not found with id: " + id));
        
        updateEntityFromDTO(existingTree, dto);
        Tree updatedTree = treeRepository.save(existingTree);
        return mapToDTO(updatedTree);
    }

    private void validateTreeData(TreeDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu giống cây trồng không được để trống.");
        }
        if (dto.getTreeName() == null || dto.getTreeName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên giống cây trồng không được để trống.");
        }
        if (dto.getHarvestDays() == null || dto.getHarvestDays() <= 0) {
            throw new IllegalArgumentException("Thời gian thu hoạch phải lớn hơn 0 ngày.");
        }
        if (dto.getMinRentalDays() == null || dto.getMinRentalDays() < dto.getHarvestDays()) {
            throw new IllegalArgumentException(String.format(
                "Số ngày thuê tối thiểu (%d ngày) không được nhỏ hơn thời gian thu hoạch (%d ngày).",
                dto.getMinRentalDays() != null ? dto.getMinRentalDays() : 0, dto.getHarvestDays()
            ));
        }
        if (dto.getPriceSmall() != null && dto.getPriceSmall().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá phôi giống Trụ Nhỏ không được là số âm.");
        }
        if (dto.getPriceMedium() != null && dto.getPriceMedium().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá phôi giống Trụ Vừa không được là số âm.");
        }
        if (dto.getPriceLarge() != null && dto.getPriceLarge().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá phôi giống Trụ Lớn không được là số âm.");
        }
        if (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá phôi giống không được là số âm.");
        }
        if (dto.getSoilMoistureMin() != null && (dto.getSoilMoistureMin() < 0 || dto.getSoilMoistureMin() > 100)) {
            throw new IllegalArgumentException("Độ ẩm đất tối thiểu phải nằm trong khoảng từ 0% đến 100%.");
        }
        if (dto.getSoilMoistureMax() != null && (dto.getSoilMoistureMax() < 0 || dto.getSoilMoistureMax() > 100)) {
            throw new IllegalArgumentException("Độ ẩm đất tối đa phải nằm trong khoảng từ 0% đến 100%.");
        }
        if (dto.getSoilMoistureMin() != null && dto.getSoilMoistureMax() != null) {
            if (dto.getSoilMoistureMax() <= dto.getSoilMoistureMin()) {
                throw new IllegalArgumentException(String.format(
                    "Độ ẩm đất tối đa (%.1f%%) phải lớn hơn độ ẩm đất tối thiểu (%.1f%%).",
                    dto.getSoilMoistureMax(), dto.getSoilMoistureMin()
                ));
            }
        }
        if (dto.getLightMin() != null && (dto.getLightMin() < 0 || dto.getLightMin() > 24)) {
            throw new IllegalArgumentException("Thời gian chiếu sáng tối thiểu phải nằm trong khoảng từ 0 đến 24 giờ.");
        }
        if (dto.getLightMax() != null && (dto.getLightMax() < 0 || dto.getLightMax() > 24)) {
            throw new IllegalArgumentException("Thời gian chiếu sáng tối đa phải nằm trong khoảng từ 0 đến 24 giờ.");
        }
        if (dto.getLightMin() != null && dto.getLightMax() != null) {
            if (dto.getLightMax() <= dto.getLightMin()) {
                throw new IllegalArgumentException(String.format(
                    "Thời gian chiếu sáng tối đa (%.1f giờ) phải lớn hơn thời gian chiếu sáng tối thiểu (%.1f giờ).",
                    dto.getLightMax(), dto.getLightMin()
                ));
            }
        }
        if (dto.getPhMin() != null && (dto.getPhMin() < 0 || dto.getPhMin() > 14)) {
            throw new IllegalArgumentException("Độ pH tối thiểu phải nằm trong khoảng từ 0 đến 14.");
        }
        if (dto.getPhMax() != null && (dto.getPhMax() < 0 || dto.getPhMax() > 14)) {
            throw new IllegalArgumentException("Độ pH tối đa phải nằm trong khoảng từ 0 đến 14.");
        }
        if (dto.getPhMin() != null && dto.getPhMax() != null) {
            if (dto.getPhMax() <= dto.getPhMin()) {
                throw new IllegalArgumentException(String.format(
                    "Độ pH tối đa (%.1f) phải lớn hơn độ pH tối thiểu (%.1f).",
                    dto.getPhMax(), dto.getPhMin()
                ));
            }
        }
    }



    @Override
    @Transactional
    public void deleteTree(Long id) {
        Tree tree = treeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree not found with id: " + id));
        tree.setIsActive(false);
        treeRepository.save(tree);
    }

    @Override
    @Transactional
    public void forceDeleteTree(Long id) {
        Tree tree = treeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree not found with id: " + id));

        List<Alert> alerts = alertRepository.findByTree(tree);
        for (Alert alert : alerts) {
            alertProcessingLogRepository.deleteAll(alertProcessingLogRepository.findByAlert(alert));
        }
        alertRepository.deleteAll(alerts);

        List<Pillar> pillarsWithDefault = pillarRepository.findByDefaultTree(tree);
        pillarsWithDefault.forEach(p -> p.setDefaultTree(null));
        pillarRepository.saveAll(pillarsWithDefault);

        List<SlotRental> rentals = slotRentalRepository.findByTree(tree);
        for (SlotRental rental : rentals) {
            paymentTransactionRepository.deleteAll(
                    paymentTransactionRepository.findByRentalIdOrderByPaymentDateDesc(rental.getId()));
            treePlantingRequestRepository.deleteAll(
                    treePlantingRequestRepository.findByRental(rental));
        }
        slotRentalRepository.deleteAll(rentals);

        treePlantingRequestRepository.deleteAll(treePlantingRequestRepository.findByNewTree(tree));

        treeRepository.delete(tree);
    }

    @Override
    public List<TreeDTO> getActiveTrees() {
        return treeRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TreeDTO mapToDTO(Tree tree) {
        TreeDTO dto = new TreeDTO();
        dto.setId(tree.getId());
        dto.setTreeName(tree.getTreeName());
        dto.setScientificName(tree.getScientificName());
        dto.setDescription(tree.getDescription());
        dto.setHarvestDays(tree.getHarvestDays());
        dto.setMinRentalDays(tree.getMinRentalDays());
        dto.setPrice(tree.getEffectivePriceSmall());
        dto.setPriceSmall(tree.getEffectivePriceSmall());
        dto.setPriceMedium(tree.getEffectivePriceMedium());
        dto.setPriceLarge(tree.getEffectivePriceLarge());
        dto.setImageUrl(tree.getImageUrl());
        dto.setSoilMoistureMin(tree.getSoilMoistureMin());
        dto.setSoilMoistureMax(tree.getSoilMoistureMax());
        dto.setLightMin(tree.getLightMin());
        dto.setLightMax(tree.getLightMax());
        dto.setPhMin(tree.getPhMin());
        dto.setPhMax(tree.getPhMax());
        dto.setCompensationPercentage(tree.getCompensationPercentage());
        dto.setCareInstructions(tree.getCareInstructions());
        dto.setIsActive(tree.getIsActive());
        return dto;
    }

    private Tree mapToEntity(TreeDTO dto) {
        Tree tree = new Tree();
        tree.setTreeName(dto.getTreeName());
        tree.setScientificName(dto.getScientificName());
        tree.setDescription(dto.getDescription());
        tree.setHarvestDays(dto.getHarvestDays());
        tree.setMinRentalDays(dto.getMinRentalDays());
        BigDecimal priceSmall = dto.getPriceSmall() != null ? dto.getPriceSmall() : dto.getPrice();
        tree.setPrice(priceSmall);
        tree.setPriceSmall(priceSmall);
        tree.setPriceMedium(dto.getPriceMedium() != null ? dto.getPriceMedium() : (priceSmall != null ? priceSmall.multiply(BigDecimal.valueOf(1.5)) : null));
        tree.setPriceLarge(dto.getPriceLarge() != null ? dto.getPriceLarge() : (priceSmall != null ? priceSmall.multiply(BigDecimal.valueOf(2.0)) : null));
        tree.setImageUrl(dto.getImageUrl());
        tree.setSoilMoistureMin(dto.getSoilMoistureMin());
        tree.setSoilMoistureMax(dto.getSoilMoistureMax());
        tree.setLightMin(dto.getLightMin());
        tree.setLightMax(dto.getLightMax());
        tree.setPhMin(dto.getPhMin());
        tree.setPhMax(dto.getPhMax());
        tree.setCompensationPercentage(dto.getCompensationPercentage());
        tree.setCareInstructions(dto.getCareInstructions());
        return tree;
    }

    private void updateEntityFromDTO(Tree tree, TreeDTO dto) {
        if (dto.getTreeName() != null) tree.setTreeName(dto.getTreeName());
        if (dto.getScientificName() != null) tree.setScientificName(dto.getScientificName());
        if (dto.getDescription() != null) tree.setDescription(dto.getDescription());
        if (dto.getHarvestDays() != null) tree.setHarvestDays(dto.getHarvestDays());
        if (dto.getMinRentalDays() != null) tree.setMinRentalDays(dto.getMinRentalDays());
        if (dto.getPriceSmall() != null) {
            tree.setPriceSmall(dto.getPriceSmall());
            tree.setPrice(dto.getPriceSmall());
        } else if (dto.getPrice() != null) {
            tree.setPrice(dto.getPrice());
            tree.setPriceSmall(dto.getPrice());
        }
        if (dto.getPriceMedium() != null) tree.setPriceMedium(dto.getPriceMedium());
        if (dto.getPriceLarge() != null) tree.setPriceLarge(dto.getPriceLarge());
        if (dto.getImageUrl() != null) tree.setImageUrl(dto.getImageUrl());
        if (dto.getSoilMoistureMin() != null) tree.setSoilMoistureMin(dto.getSoilMoistureMin());
        if (dto.getSoilMoistureMax() != null) tree.setSoilMoistureMax(dto.getSoilMoistureMax());
        if (dto.getLightMin() != null) tree.setLightMin(dto.getLightMin());
        if (dto.getLightMax() != null) tree.setLightMax(dto.getLightMax());
        if (dto.getPhMin() != null) tree.setPhMin(dto.getPhMin());
        if (dto.getPhMax() != null) tree.setPhMax(dto.getPhMax());
        if (dto.getCompensationPercentage() != null) tree.setCompensationPercentage(dto.getCompensationPercentage());
        if (dto.getCareInstructions() != null) tree.setCareInstructions(dto.getCareInstructions());
        if (dto.getIsActive() != null) tree.setIsActive(dto.getIsActive());
    }

}
