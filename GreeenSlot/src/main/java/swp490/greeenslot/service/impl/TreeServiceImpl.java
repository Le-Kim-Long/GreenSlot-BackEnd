package swp490.greeenslot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp490.greeenslot.dto.TreeDTO;
import swp490.greeenslot.entity.Tree;
import swp490.greeenslot.repository.TreeRepository;
import swp490.greeenslot.service.TreeService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TreeServiceImpl implements TreeService {

    @Autowired
    private TreeRepository treeRepository;

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
        Tree tree = mapToEntity(dto);
        tree.setIsActive(true);
        Tree savedTree = treeRepository.save(tree);
        return mapToDTO(savedTree);
    }

    @Override
    @Transactional
    public TreeDTO updateTree(Long id, TreeDTO dto) {
        Tree existingTree = treeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tree not found with id: " + id));
        
        updateEntityFromDTO(existingTree, dto);
        Tree updatedTree = treeRepository.save(existingTree);
        return mapToDTO(updatedTree);
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
    public List<TreeDTO> getActiveTrees() {
        return treeRepository.findByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TreeDTO mapToDTO(Tree tree) {
        return new TreeDTO(
                tree.getId(),
                tree.getTreeName(),
                tree.getScientificName(),
                tree.getDescription(),
                tree.getHarvestDays(),
                tree.getMinRentalDays(),
                tree.getPrice(),
                tree.getImageUrl(),
                tree.getSoilMoistureMin(),
                tree.getSoilMoistureMax(),
                tree.getLightMin(),
                tree.getLightMax(),
                tree.getPhMin(),
                tree.getPhMax(),
                tree.getCompensationPercentage(),
                tree.getCareInstructions(),
                tree.getIsActive()
        );
    }

    private Tree mapToEntity(TreeDTO dto) {
        Tree tree = new Tree();
        tree.setTreeName(dto.getTreeName());
        tree.setScientificName(dto.getScientificName());
        tree.setDescription(dto.getDescription());
        tree.setHarvestDays(dto.getHarvestDays());
        tree.setMinRentalDays(dto.getMinRentalDays());
        tree.setPrice(dto.getPrice());
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
        if (dto.getPrice() != null) tree.setPrice(dto.getPrice());
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
