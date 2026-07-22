package swp490.greeenslot.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import swp490.greeenslot.entity.ImageFile;
import swp490.greeenslot.entity.ImageStatus;
import swp490.greeenslot.entity.User;

import java.util.List;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {

    List<ImageFile> findByStatus(ImageStatus status);

    List<ImageFile> findByUploadedByAndStatus(User uploadedBy, ImageStatus status);
}