package swp490.greeenslot.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FirebaseStorageService {

    String uploadImage(MultipartFile file) throws IOException;

    String uploadEvidence(MultipartFile file) throws IOException;

    String uploadAvatar(MultipartFile file) throws IOException;

    String uploadVideo(MultipartFile file) throws IOException;

    void deleteFile(String fileName) throws IOException;

    String getFileUrl(String fileName);

    boolean fileExists(String fileName);
}
