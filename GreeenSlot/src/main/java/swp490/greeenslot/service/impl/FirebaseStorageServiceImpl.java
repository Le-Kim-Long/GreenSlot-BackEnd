package swp490.greeenslot.service.impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import swp490.greeenslot.service.FirebaseStorageService;

import java.io.IOException;
import java.util.UUID;

@Service
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    @Value("${firebase.storage.bucket:greenslot.appspot.com}")
    private String bucketName;

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp"
    };

    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "webp"
    };

    private static final String[] ALLOWED_VIDEO_TYPES = {
            "video/mp4", "video/quicktime", "video/x-msvideo", "video/x-ms-wmv"
    };

    private static final String[] ALLOWED_VIDEO_EXTENSIONS = {
            "mp4", "mov", "avi", "wmv"
    };

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        String fileName = "images/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        return uploadToFirebase(file, fileName);
    }

    @Override
    public String uploadEvidence(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        String fileName = "evidence/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        return uploadToFirebase(file, fileName);
    }

    @Override
    public String uploadAvatar(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        String fileName = "avatars/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        return uploadToFirebase(file, fileName);
    }

    @Override
    public String uploadVideo(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_VIDEO_TYPES, ALLOWED_VIDEO_EXTENSIONS, MAX_VIDEO_SIZE);
        String fileName = "videos/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        return uploadToFirebase(file, fileName);
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName);
            StorageClient.getInstance().bucket().getStorage().delete(blobId);
        } catch (Exception e) {
            throw new IOException("Failed to delete file: " + fileName, e);
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        Blob blob = StorageClient.getInstance().bucket().get(fileName);
        if (blob == null) {
            return null;
        }
        return String.format("https://storage.googleapis.com/%s/%s", blob.getBucket(), blob.getName());
    }

    @Override
    public boolean fileExists(String fileName) {
        Blob blob = StorageClient.getInstance().bucket().get(fileName);
        return blob != null && blob.exists();
    }

    private String uploadToFirebase(MultipartFile file, String fileName) throws IOException {
        try {
            Blob blob = StorageClient.getInstance()
                    .bucket()
                    .create(fileName, file.getBytes(), file.getContentType());

            return String.format(
                    "https://storage.googleapis.com/%s/%s",
                    blob.getBucket(),
                    blob.getName()
            );
        } catch (Exception e) {
            throw new IOException("Failed to upload file to Firebase Storage", e);
        }
    }

    private void validateFile(MultipartFile file, String[] allowedTypes, String[] allowedExtensions, long maxSize) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds maximum allowed size of " + (maxSize / 1024 / 1024) + "MB");
        }

        // Validate Content Type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType, allowedTypes)) {
            throw new IOException("File type not allowed. Allowed types: " + String.join(", ", allowedTypes));
        }

        // Validate Extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isAllowedExtension(originalFilename, allowedExtensions)) {
            throw new IOException("File extension not allowed. Allowed extensions: " + String.join(", ", allowedExtensions));
        }
    }

    private boolean isAllowedType(String contentType, String[] allowedTypes) {
        for (String allowedType : allowedTypes) {
            if (contentType.equalsIgnoreCase(allowedType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedExtension(String fileName, String[] allowedExtensions) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return false;
        }
        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        for (String allowedExtension : allowedExtensions) {
            if (extension.equals(allowedExtension)) {
                return true;
            }
        }
        return false;
    }
}