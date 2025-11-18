package com.medicine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path profileStorageLocation;
    private final Path imageStorageLocation;
    private final Path mealStorageLocation;
    private final Path pointItemStorageLocation;
    private final Path dailyStorageLocation;

    public FileStorageService() {
        // Get the directory where the JAR is running
        String baseDir = System.getProperty("user.dir");

        this.profileStorageLocation = Paths.get(baseDir, "profile").toAbsolutePath().normalize();
        this.imageStorageLocation = Paths.get(baseDir, "image").toAbsolutePath().normalize();
        this.mealStorageLocation = Paths.get(baseDir, "meal").toAbsolutePath().normalize();
        this.pointItemStorageLocation = Paths.get(baseDir, "pointitem").toAbsolutePath().normalize();
        this.dailyStorageLocation = Paths.get(baseDir, "daily").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.profileStorageLocation);
            Files.createDirectories(this.imageStorageLocation);
            Files.createDirectories(this.mealStorageLocation);
            Files.createDirectories(this.pointItemStorageLocation);
            Files.createDirectories(this.dailyStorageLocation);
            log.info("Storage directories created/verified:");
            log.info("  Profile: {}", this.profileStorageLocation);
            log.info("  Image: {}", this.imageStorageLocation);
            log.info("  Meal: {}", this.mealStorageLocation);
            log.info("  PointItem: {}", this.pointItemStorageLocation);
            log.info("  Daily: {}", this.dailyStorageLocation);
        } catch (Exception ex) {
            log.error("Could not create storage directories", ex);
            throw new RuntimeException("Could not create storage directories", ex);
        }
    }

    /**
     * Store profile image
     *
     * @param file   the file to store
     * @param userId the user ID
     * @return the relative path to the stored file (e.g., "/files/profile/user_123_abc.jpg")
     */
    public String storeProfileImage(MultipartFile file, String userId) throws IOException {
        return storeFile(file, profileStorageLocation, "profile", userId);
    }

    /**
     * Store comment image
     *
     * @param file      the file to store
     * @param commentId the comment ID
     * @return the relative path to the stored file (e.g., "/files/image/comment_123_abc.jpg")
     */
    public String storeCommentImage(MultipartFile file, String commentId) throws IOException {
        return storeFile(file, imageStorageLocation, "image", commentId);
    }

    /**
     * Store meal image
     *
     * @param file   the file to store
     * @param mealId the meal ID
     * @return the relative path to the stored file (e.g., "/files/meal/meal_123_abc.jpg")
     */
    public String storeMealImage(MultipartFile file, String mealId) throws IOException {
        return storeFile(file, mealStorageLocation, "meal", mealId);
    }

    /**
     * Store point item image
     *
     * @param file the file to store
     * @return the relative path to the stored file (e.g., "/files/pointitem/item_timestamp_uuid.jpg")
     */
    public String storePointItemImage(MultipartFile file) throws IOException {
        String itemId = "item";
        return storeFile(file, pointItemStorageLocation, "pointitem", itemId);
    }

    /**
     * Store daily media (image or video)
     *
     * @param file the file to store
     * @return the relative path to the stored file (e.g., "/files/daily/daily_timestamp_uuid.jpg")
     */
    public String storeDailyMedia(MultipartFile file) throws IOException {
        String dailyId = "daily";
        return storeFile(file, dailyStorageLocation, "daily", dailyId);
    }

    /**
     * Generic file storage method
     */
    private String storeFile(MultipartFile file, Path storageLocation, String type, String identifier) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IOException("Invalid filename: " + originalFilename);
        }

        // Get file extension
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFilename.substring(lastDotIndex);
        }

        // Generate unique filename: {identifier}_{timestamp}_{uuid}.{extension}
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String filename = String.format("%s_%s_%s%s", identifier, timestamp, uuid, extension);

        // Store file
        Path targetLocation = storageLocation.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        log.info("File stored successfully: type={}, filename={}, size={} bytes",
                type, filename, file.getSize());

        // Return relative URL path
        return "/files/" + type + "/" + filename;
    }

    /**
     * Delete file
     *
     * @param filePath the relative path (e.g., "/files/profile/user_123_abc.jpg")
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }

        try {
            // Parse the file path
            // Expected format: /files/{type}/{filename}
            String[] parts = filePath.split("/");
            if (parts.length < 4) {
                log.warn("Invalid file path format: {}", filePath);
                return;
            }

            String type = parts[2]; // "profile" or "image"
            String filename = parts[3];

            Path fileLocation;
            if ("profile".equals(type)) {
                fileLocation = profileStorageLocation.resolve(filename);
            } else if ("image".equals(type)) {
                fileLocation = imageStorageLocation.resolve(filename);
            } else if ("meal".equals(type)) {
                fileLocation = mealStorageLocation.resolve(filename);
            } else if ("pointitem".equals(type)) {
                fileLocation = pointItemStorageLocation.resolve(filename);
            } else if ("daily".equals(type)) {
                fileLocation = dailyStorageLocation.resolve(filename);
            } else {
                log.warn("Unknown file type: {}", type);
                return;
            }

            Files.deleteIfExists(fileLocation);
            log.info("File deleted successfully: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", filePath, e);
        }
    }

    /**
     * Get file path for serving
     */
    public Path getFilePath(String type, String filename) {
        if ("profile".equals(type)) {
            return profileStorageLocation.resolve(filename);
        } else if ("image".equals(type)) {
            return imageStorageLocation.resolve(filename);
        } else if ("meal".equals(type)) {
            return mealStorageLocation.resolve(filename);
        } else if ("pointitem".equals(type)) {
            return pointItemStorageLocation.resolve(filename);
        } else if ("daily".equals(type)) {
            return dailyStorageLocation.resolve(filename);
        }
        throw new IllegalArgumentException("Unknown file type: " + type);
    }
}
