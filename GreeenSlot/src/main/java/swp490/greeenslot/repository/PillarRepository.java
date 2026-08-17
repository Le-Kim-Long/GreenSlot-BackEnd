package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.Pillar;
import swp490.greeenslot.entity.Tree;

@Repository
public interface PillarRepository extends JpaRepository<Pillar, Long> {
    boolean existsByLocationId(Long locationId);

    java.util.Optional<Pillar> findByPillarCode(String pillarCode);

    java.util.List<Pillar> findByDefaultTree(Tree tree);
}
