package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.Tree;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreeRepository extends JpaRepository<Tree, Long> {
    
    List<Tree> findByIsActiveTrue();
    
    Optional<Tree> findByIdAndIsActiveTrue(Long id);
    
    List<Tree> findByTreeNameContainingIgnoreCase(String name);
    
    Optional<Tree> findByTreeName(String treeName);
}
