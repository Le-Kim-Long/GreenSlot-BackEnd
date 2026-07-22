package swp490.greeenslot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swp490.greeenslot.dto.ImageFileDTO;
import swp490.greeenslot.dto.ImageUploadResponseDTO;
import swp490.greeenslot.dto.MessageResponseDTO;
import swp490.greeenslot.entity.ImageFile;
import swp490.greeenslot.entity.ImageStatus;
import swp490.greeenslot.entity.User;
import swp490.greeenslot.repository.ImageFileRepository;
import swp490.greeenslot.service.FirebaseStorageService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://greenslot-frontend4.vercel.app", "*"}, maxAge = 3600)
@Tag(name = "Image Management", description = "APIs for uploading and managing images and videos")
public class ImageController {

    private final FirebaseStorageService firebaseStorageService;
    private final ImageFileRepository imageFileRepository;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload image", description = "Upload an image to Firebase Storage")
    public ResponseEntity<ImageUploadResponseDTO> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            String publicUrl = firebaseStorageService.uploadImage(file);
            
            ImageFile imageFile = ImageFile.builder()
                    .fileName(file.getOriginalFilename())
                    .storagePath(extractStoragePath(publicUrl))
                    .publicUrl(publicUrl)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .status(ImageStatus.ACTIVE)
                    .uploadedBy(currentUser)
                    .build();
            
            imageFile = imageFileRepository.save(imageFile);
            
            ImageUploadResponseDTO response = ImageUploadResponseDTO.builder()
                    .id(imageFile.getId())
                    .fileName(imageFile.getFileName())
                    .publicUrl(imageFile.getPublicUrl())
                    .contentType(imageFile.getContentType())
                    .fileSize(imageFile.getFileSize())
                    .message("Image uploaded successfully")
                    .uploadType("IMAGE")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ImageUploadResponseDTO.builder()
                            .message("Failed to upload image: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/upload/evidence")
    @PreAuthorize("hasAnyRole('ROLE_GARDEN_STAFF', 'ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Upload evidence image", description = "Upload evidence image for task completion")
    public ResponseEntity<ImageUploadResponseDTO> uploadEvidence(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            String publicUrl = firebaseStorageService.uploadEvidence(file);
            
            ImageFile imageFile = ImageFile.builder()
                    .fileName(file.getOriginalFilename())
                    .storagePath(extractStoragePath(publicUrl))
                    .publicUrl(publicUrl)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .status(ImageStatus.ACTIVE)
                    .uploadedBy(currentUser)
                    .build();
            
            imageFile = imageFileRepository.save(imageFile);
            
            ImageUploadResponseDTO response = ImageUploadResponseDTO.builder()
                    .id(imageFile.getId())
                    .fileName(imageFile.getFileName())
                    .publicUrl(imageFile.getPublicUrl())
                    .contentType(imageFile.getContentType())
                    .fileSize(imageFile.getFileSize())
                    .message("Evidence uploaded successfully")
                    .uploadType("EVIDENCE")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ImageUploadResponseDTO.builder()
                            .message("Failed to upload evidence: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/upload/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload avatar", description = "Upload user avatar image")
    public ResponseEntity<ImageUploadResponseDTO> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            String publicUrl = firebaseStorageService.uploadAvatar(file);
            
            ImageFile imageFile = ImageFile.builder()
                    .fileName(file.getOriginalFilename())
                    .storagePath(extractStoragePath(publicUrl))
                    .publicUrl(publicUrl)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .status(ImageStatus.ACTIVE)
                    .uploadedBy(currentUser)
                    .build();
            
            imageFile = imageFileRepository.save(imageFile);
            
            ImageUploadResponseDTO response = ImageUploadResponseDTO.builder()
                    .id(imageFile.getId())
                    .fileName(imageFile.getFileName())
                    .publicUrl(imageFile.getPublicUrl())
                    .contentType(imageFile.getContentType())
                    .fileSize(imageFile.getFileSize())
                    .message("Avatar uploaded successfully")
                    .uploadType("AVATAR")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ImageUploadResponseDTO.builder()
                            .message("Failed to upload avatar: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/upload/video")
    @PreAuthorize("hasAnyRole('ROLE_GARDEN_STAFF', 'ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Upload video", description = "Upload video file to Firebase Storage")
    public ResponseEntity<ImageUploadResponseDTO> uploadVideo(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            String publicUrl = firebaseStorageService.uploadVideo(file);
            
            ImageFile imageFile = ImageFile.builder()
                    .fileName(file.getOriginalFilename())
                    .storagePath(extractStoragePath(publicUrl))
                    .publicUrl(publicUrl)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .status(ImageStatus.ACTIVE)
                    .uploadedBy(currentUser)
                    .build();
            
            imageFile = imageFileRepository.save(imageFile);
            
            ImageUploadResponseDTO response = ImageUploadResponseDTO.builder()
                    .id(imageFile.getId())
                    .fileName(imageFile.getFileName())
                    .publicUrl(imageFile.getPublicUrl())
                    .contentType(imageFile.getContentType())
                    .fileSize(imageFile.getFileSize())
                    .message("Video uploaded successfully")
                    .uploadType("VIDEO")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ImageUploadResponseDTO.builder()
                            .message("Failed to upload video: " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get image by ID", description = "Get image metadata by ID")
    public ResponseEntity<ImageFileDTO> getImageById(@PathVariable Long id) {
        return imageFileRepository.findById(id)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @Operation(summary = "Get all images", description = "Get all images (admin only)")
    public ResponseEntity<List<ImageFileDTO>> getAllImages() {
        List<ImageFileDTO> images = imageFileRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN')")
    @Operation(summary = "Get active images", description = "Get all active images")
    public ResponseEntity<List<ImageFileDTO>> getActiveImages() {
        List<ImageFileDTO> images = imageFileRepository.findByStatus(ImageStatus.ACTIVE).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }

    @GetMapping("/my-uploads")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my uploads", description = "Get all images uploaded by current user")
    public ResponseEntity<List<ImageFileDTO>> getMyUploads(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<ImageFileDTO> images = imageFileRepository.findByUploadedByAndStatus(currentUser, ImageStatus.ACTIVE).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete image", description = "Delete image by ID (soft delete)")
    public ResponseEntity<MessageResponseDTO> deleteImage(
            @PathVariable Long id,
            Authentication authentication) {
        return imageFileRepository.findById(id)
                .map(imageFile -> {
                    try {
                        // Check if user is the uploader or has admin role
                        User currentUser = (User) authentication.getPrincipal();
                        boolean isAdmin = currentUser.getRoles().stream()
                                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN") || 
                                              role.getName().name().equals("ROLE_MANAGER"));
                        
                        if (!isAdmin && !imageFile.getUploadedBy().getId().equals(currentUser.getId())) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(new MessageResponseDTO("You don't have permission to delete this image"));
                        }
                        
                        // Soft delete - mark as deleted
                        imageFile.setStatus(ImageStatus.DELETED);
                        imageFileRepository.save(imageFile);
                        
                        // Optionally delete from Firebase
                        // firebaseStorageService.deleteFile(imageFile.getStoragePath());
                        
                        return ResponseEntity.ok(new MessageResponseDTO("Image deleted successfully"));
                    } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                .body(new MessageResponseDTO("Failed to delete image: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private ImageFileDTO mapToDTO(ImageFile imageFile) {
        return ImageFileDTO.builder()
                .id(imageFile.getId())
                .fileName(imageFile.getFileName())
                .storagePath(imageFile.getStoragePath())
                .publicUrl(imageFile.getPublicUrl())
                .contentType(imageFile.getContentType())
                .fileSize(imageFile.getFileSize())
                .uploadedAt(imageFile.getUploadedAt())
                .status(imageFile.getStatus().name())
                .uploadedBy(imageFile.getUploadedBy() != null ? imageFile.getUploadedBy().getId() : null)
                .uploadedByUsername(imageFile.getUploadedBy() != null ? imageFile.getUploadedBy().getUsername() : null)
                .build();
    }

    private String extractStoragePath(String publicUrl) {
        // Extract the storage path from the public URL
        // URL format: https://storage.googleapis.com/bucket-name/path/to/file
        String[] parts = publicUrl.split("/");
        if (parts.length >= 5) {
            StringBuilder path = new StringBuilder();
            for (int i = 4; i < parts.length; i++) {
                if (path.length() > 0) path.append("/");
                path.append(parts[i]);
            }
            return path.toString();
        }
        return publicUrl;
    }
}