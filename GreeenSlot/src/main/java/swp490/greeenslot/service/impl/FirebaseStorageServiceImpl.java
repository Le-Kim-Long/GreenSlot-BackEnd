package swp490.greeenslot.service.impl;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import swp490.greeenslot.service.FirebaseStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    @Value("${firebase.storage.bucket:greenslot.appspot.com}")
    private String bucketName;

    private static final long MAX_IMAGE_SIZE = 50 * 1024 * 1024; // 50MB (supports all high-resolution camera images)
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/heic", "image/heif", "image/jfif", "image/svg+xml", "image/tiff", "image/x-icon"
    };

    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "jfif", "svg", "tiff", "ico", "dng", "raw"
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
        return uploadToStorage(file, "general");
    }

    @Override
    public String uploadTree(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        return uploadToStorage(file, "trees");
    }

    @Override
    public String uploadEquipment(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        return uploadToStorage(file, "equipment");
    }

    @Override
    public String uploadEvidence(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        return uploadToStorage(file, "evidence");
    }

    @Override
    public String uploadAvatar(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        return uploadToStorage(file, "avatars");
    }

    @Override
    public String uploadVideo(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_VIDEO_TYPES, ALLOWED_VIDEO_EXTENSIONS, MAX_VIDEO_SIZE);
        return uploadToStorage(file, "videos");
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                BlobId blobId = BlobId.of(bucketName, fileName);
                StorageClient.getInstance().bucket().getStorage().delete(blobId);
            }
        } catch (Exception e) {
            System.err.println("WARN: Firebase delete failed, ignoring: " + e.getMessage());
        }

        // Delete local copy if exists
        try {
            Path localPath = Paths.get("uploads", fileName);
            Files.deleteIfExists(localPath);
        } catch (Exception ignored) {}
    }

    @Override
    public String getFileUrl(String fileName) {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                Blob blob = StorageClient.getInstance().bucket().get(fileName);
                if (blob != null) {
                    return String.format("https://storage.googleapis.com/%s/%s", blob.getBucket(), blob.getName());
                }
            }
        } catch (Exception ignored) {}

        // Fallback to local image URL
        try {
            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            return baseUrl + "/api/images/view/" + fileName;
        } catch (Exception e) {
            return "/api/images/view/" + fileName;
        }
    }

    @Override
    public boolean fileExists(String fileName) {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                Blob blob = StorageClient.getInstance().bucket().get(fileName);
                if (blob != null && blob.exists()) return true;
            }
        } catch (Exception ignored) {}

        Path localPath = Paths.get("uploads", fileName);
        return Files.exists(localPath);
    }

    private String uploadToStorage(MultipartFile file, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeFilename = UUID.randomUUID() + "_" + (originalFilename != null ? originalFilename.replaceAll("\\s+", "_") : "image.jpg");
        String relativePath = folder + "/" + safeFilename;

        // 1. Try Firebase Storage if initialized
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                Blob blob = StorageClient.getInstance()
                        .bucket()
                        .create(relativePath, file.getBytes(), file.getContentType());

                return String.format(
                        "https://storage.googleapis.com/%s/%s",
                        blob.getBucket(),
                        blob.getName()
                );
            }
        } catch (Exception e) {
            System.err.println("INFO: Firebase storage not connected (" + e.getMessage() + "), saving file to local disk...");
        }

        // 2. Local disk storage (guaranteed to store actual file bytes)
        try {
            Path dirPath = Paths.get("uploads", folder);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path targetPath = dirPath.resolve(safeFilename);
            Files.write(targetPath, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String baseUrl;
            try {
                baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            } catch (Exception ex) {
                baseUrl = "http://localhost:8080";
            }
            return baseUrl + "/api/images/view/" + folder + "/" + safeFilename;
        } catch (Exception ex) {
            System.err.println("ERROR: Failed to save file locally: " + ex.getMessage());
            throw new IOException("Failed to save file: " + ex.getMessage(), ex);
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
        if (contentType == null) return false;
        if (contentType.toLowerCase().startsWith("image/")) return true;
        for (String allowedType : allowedTypes) {
            if (contentType.equalsIgnoreCase(allowedType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedExtension(String fileName, String[] allowedExtensions) {
        if (fileName == null) return false;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return false;
        }
        String extension = fileName.substring(lastDotIndex + 1).toLowerCase();
        for (String allowedExtension : allowedExtensions) {
            if (extension.equalsIgnoreCase(allowedExtension)) {
                return true;
            }
        }
        return false;
    }
}