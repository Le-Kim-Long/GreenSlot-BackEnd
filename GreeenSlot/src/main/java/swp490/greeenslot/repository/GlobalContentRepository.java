package swp490.greeenslot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp490.greeenslot.entity.EContentType;
import swp490.greeenslot.entity.GlobalContent;
import java.util.List;

@Repository
public interface GlobalContentRepository extends JpaRepository<GlobalContent, Long> {
    List<GlobalContent> findByContentTypeAndActiveTrue(EContentType contentType);
}
