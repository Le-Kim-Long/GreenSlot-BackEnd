package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.Equipment;
import swp490.greeenslot.entity.EEquipmentStatus;
import swp490.greeenslot.entity.Pillar;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    
    List<Equipment> findByPillar(Pillar pillar);
    
    List<Equipment> findByStatus(EEquipmentStatus status);
    
    List<Equipment> findByPillarAndStatus(Pillar pillar, EEquipmentStatus status);
}
