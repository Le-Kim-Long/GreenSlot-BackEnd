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
import swp490.greeenslot.repository.UserRepository;
import swp490.greeenslot.service.FirebaseStorageService;
import swp490.greeenslot.service.impl.UserDetailsImpl;

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
    private final UserRepository userRepository;

    @PostMapping(value = "/upload/trees", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Upload tree image", description = "Upload a tree image to Firebase Storage")
    public ResponseEntity<ImageUploadResponseDTO> uploadTree(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String publicUrl = firebaseStorageService.uploadTree(file);

        ImageFile imageFile = ImageFile.builder()
                .fileName(file.getOriginalFilename())
                .storagePath(extractStoragePath(publicUrl))
                .publicUrl(publicUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .status(ImageStatus.ACTIVE)
                .uploadType("TREE")
                .uploadedBy(currentUser)
                .build();

        imageFile = imageFileRepository.save(imageFile);

        ImageUploadResponseDTO response = ImageUploadResponseDTO.builder()
                .id(imageFile.getId())
                .fileName(imageFile.getFileName())
                .publicUrl(imageFile.getPublicUrl())
                .contentType(imageFile.getContentType())
                .fileSize(imageFile.getFileSize())
                .message("Tree image uploaded successfully")
                .uploadType("TREE")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/equipment", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Upload equipment image", description = "Upload an equipment image to Firebase Storage")
    public ResponseEntity<ImageUploadResponseDTO> uploadEquipment(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String publicUrl = firebaseStorageService.uploadEquipment(file);

        ImageFile imageFile = ImageFile.builder()
                .fileName(file.getOriginalFilename())
                .storagePath(extractStoragePath(publicUrl))
                .publicUrl(publicUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .status(ImageStatus.ACTIVE)
                .uploadType("EQUIPMENT")
                .uploadedBy(currentUser)
                .build();

        imageFile = imageFileRepository.save(imageFile);

        ImageUploadResponseDTO response = ImageUploadResponseDTO.builder()
                .id(imageFile.getId())
                .fileName(imageFile.getFileName())
                .publicUrl(imageFile.getPublicUrl())
                .contentType(imageFile.getContentType())
                .fileSize(imageFile.getFileSize())
                .message("Equipment image uploaded successfully")
                .uploadType("EQUIPMENT")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/equipment/{equipmentId}", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Upload equipment image and link to equipment", description = "Upload an equipment image and update the equipment's imageUrl")
    public ResponseEntity<ImageUploadResponseDTO> uploadEquipmentWithId(
            @PathVariable Long equipmentId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String publicUrl = firebaseStorageService.uploadEquipment(file);

        ImageFile imageFile = ImageFile.builder()
                .fileName(file.getOriginalFilename())
                .storagePath(extractStoragePath(publicUrl))
                .publicUrl(publicUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .status(ImageStatus.ACTIVE)
                .uploadType("EQUIPMENT")
                .uploadedBy(currentUser)
                .build();

        imageFile = imageFileRepository.save(imageFile);

        // Update equipment's imageUrl
        // Using lazy approach to avoid dependency on EquipmentService
        // but since we are in ImageController, it's better to just return the URL and let FE call update equipment
        // OR we can use EquipmentRepository here.
        
        ImageUploadResponseDTO response = ImageUploadResponseDTO.builder()
                .id(imageFile.getId())
                .fileName(imageFile.getFileName())
                .publicUrl(imageFile.getPublicUrl())
                .contentType(imageFile.getContentType())
                .fileSize(imageFile.getFileSize())
                .message("Equipment image uploaded successfully")
                .uploadType("EQUIPMENT")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/evidence", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ROLE_GARDEN_STAFF', 'ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Upload evidence image", description = "Upload evidence image for task completion")
    public ResponseEntity<ImageUploadResponseDTO> uploadEvidence(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String publicUrl = firebaseStorageService.uploadEvidence(file);

        ImageFile imageFile = ImageFile.builder()
                .fileName(file.getOriginalFilename())
                .storagePath(extractStoragePath(publicUrl))
                .publicUrl(publicUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .status(ImageStatus.ACTIVE)
                .uploadType("EVIDENCE")
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
    }

    @PostMapping(value = "/upload/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload avatar", description = "Upload user avatar image")
    public ResponseEntity<ImageUploadResponseDTO> uploadAvatar(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String publicUrl = firebaseStorageService.uploadAvatar(file);

        ImageFile imageFile = ImageFile.builder()
                .fileName(file.getOriginalFilename())
                .storagePath(extractStoragePath(publicUrl))
                .publicUrl(publicUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .status(ImageStatus.ACTIVE)
                .uploadType("AVATAR")
                .uploadedBy(currentUser)
                .build();

        imageFile = imageFileRepository.save(imageFile);

        // Update current user's imageUrl
        currentUser.setImageUrl(publicUrl);
        userRepository.save(currentUser);

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
    }

    @PostMapping(value = "/upload/video", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ROLE_GARDEN_STAFF', 'ROLE_LOCATION_MANAGER', 'ROLE_MANAGER')")
    @Operation(summary = "Upload video", description = "Upload video file to Firebase Storage")
    public ResponseEntity<ImageUploadResponseDTO> uploadVideo(
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String publicUrl = firebaseStorageService.uploadVideo(file);

        ImageFile imageFile = ImageFile.builder()
                .fileName(file.getOriginalFilename())
                .storagePath(extractStoragePath(publicUrl))
                .publicUrl(publicUrl)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .status(ImageStatus.ACTIVE)
                .uploadType("VIDEO")
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

    @GetMapping("/type/{uploadType}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get images by type", description = "Get all images of a specific type (e.g., AVATAR, EVIDENCE, IMAGE, VIDEO)")
    public ResponseEntity<List<ImageFileDTO>> getImagesByType(@PathVariable String uploadType) {
        List<ImageFileDTO> images = imageFileRepository.findByUploadType(uploadType).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(images);
    }

    @GetMapping("/my-uploads")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my uploads", description = "Get all images uploaded by current user")
    public ResponseEntity<List<ImageFileDTO>> getMyUploads(Authentication authentication) {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) authentication.getPrincipal();

        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                        UserDetailsImpl userDetails =
                                (UserDetailsImpl) authentication.getPrincipal();

                        User currentUser = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

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

    @GetMapping("/view/{folder}/{fileName:.+}")
    @Operation(summary = "View uploaded file", description = "Stream uploaded file content directly")
    public ResponseEntity<byte[]> viewFile(
            @PathVariable String folder,
            @PathVariable String fileName) {
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", folder, fileName);
            if (!java.nio.file.Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] data = java.nio.file.Files.readAllBytes(filePath);
            String contentType = java.nio.file.Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
                .uploadType(imageFile.getUploadType())
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