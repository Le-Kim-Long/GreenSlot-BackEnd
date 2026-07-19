package swp490.greeenslot.service;

import swp490.greeenslot.dto.TreeDTO;

import java.util.List;

public interface TreeService {
    
    List<TreeDTO> getAllTrees();
    
    TreeDTO getTreeById(Long id);
    
    TreeDTO createTree(TreeDTO dto);
    
    TreeDTO updateTree(Long id, TreeDTO dto);
    
    void deleteTree(Long id);
    
    List<TreeDTO> getActiveTrees();
}
