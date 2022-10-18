package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.config.AppProperties;
import cu.sld.ucmgt.directory.service.error.StorageException;
import io.minio.*;
import io.minio.errors.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;

/**
 * Storage service interface
 */
@Slf4j
@Service
public class FileService {

    private final Path rootLocation;
    private final MinioClient minioClient;
    @Value("${minio.bucket.name}")
    private String bucketName;

    public FileService(AppProperties appProperties, MinioClient minioClient) {
        this.rootLocation = Paths.get(appProperties.getStorage().getUploadDir()).toAbsolutePath().normalize();
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void initS3() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (bucketExists) {
                log.info(String.format("Using existing Bucket [%s]", bucketName));
            } else {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info(String.format("Create Bucket %s", bucketName));
            }
        } catch (InvalidKeyException |
                IOException |
                InsufficientDataException |
                ServerException |
                InvalidResponseException |
                ErrorResponseException |
                NoSuchAlgorithmException |
                InternalException |
                XmlParserException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save file uploaded to server
     * @param fileInput file to save
     * @return String filename
     */
    public String store(MultipartFile fileInput, String name) {
        // Normalize file name
        Optional<MultipartFile> file = Optional.ofNullable(fileInput);
        if (file.isPresent()) {
            String filename = "avatar";
            if (name != null) {
                filename = name;
            } else if (!Objects.equals(file.get().getOriginalFilename(), "")){
                filename = Objects.requireNonNull(file.get().getOriginalFilename())
                    .replaceAll("[^0-9_a-zA-Z\\(\\)\\%\\-\\.]", "");
            }
            try {
                if (file.get().isEmpty()) {
                    throw new StorageException("Can not store empty file " + filename);
                }
                //Check if the file's name contains invalid characters(Security)
                if (filename.contains("..")) {
                    throw new StorageException("Can not storage with relative path outside current directory " + filename);
                }
                // Copy file to the target location (Replacing existing file with the same name)
                try (InputStream inputStream = file.get().getInputStream()) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(inputStream, fileInput.getSize(), -1)
                            .contentType(fileInput.getContentType())
                            .build());
                }
                return filename;
            } catch (Exception e) {
                throw new StorageException("Failed to store file " + filename, e);
            }
        } else {
            throw new StorageException("Input file not exist");
        }
    }

    /**
     *  Remove file if exists
     * @param filename file name
     * @return True if the file was deleted, False otherwise
     */
    public Boolean delete(String filename) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(filename).build());
            return true;
        } catch (IOException
                | NoSuchAlgorithmException
                | InvalidKeyException
                | InvalidResponseException
                | InsufficientDataException
                | ServerException
                | InternalException
                | XmlParserException
                | ErrorResponseException exception) {
            log.error("Could not delete file", exception);
        }
        return false;
    }

    /**
     * Event to store a new avatar belong to Employee ,Student or Workplace
     * condition fileInput not null && oldFileName blank && newFileName not blank
     * @param saveFileEvent event
     */
    @EventListener(condition = "#saveFileEvent.getFileInput() != null " +
            "&& #saveFileEvent.getNewFileName() != null " +
            "&& (#saveFileEvent.getOldFileName() == null || #saveFileEvent.getOldFileName().isBlank())")
    public void storeNewAvatar(SaveFileEvent saveFileEvent) {
        if (!saveFileEvent.getNewFileName().isBlank()) {
            store(saveFileEvent.getFileInput(), saveFileEvent.getNewFileName().toLowerCase());
        }
    }

    /**
     * Event to update a avatar belong to Employee ,Student or Workplace
     * condition fileInput not null && oldFileName not blank && newFileName not blank
     * @param saveFileEvent event
     */
    @EventListener(condition = "#saveFileEvent.getFileInput() != null " +
            "&& #saveFileEvent.getOldFileName() != null " +
            "&& #saveFileEvent.getNewFileName() != null")
    public void updateAvatar(SaveFileEvent saveFileEvent) {
        if (!saveFileEvent.getOldFileName().isBlank() && !saveFileEvent.getNewFileName().isBlank()) {
            boolean result = delete(saveFileEvent.getOldFileName().toLowerCase());
            if (result) {
                store(saveFileEvent.getFileInput(), saveFileEvent.getNewFileName().toLowerCase());
            }
        }
    }

    /**
     * Event to rename a avatar belong to Employee ,Student or Workplace
     * condition null fileInput && oldFileName not blank && newFileName not blank
     * @param saveFileEvent event
     */
    @EventListener(condition = "#saveFileEvent.getFileInput() == null " +
            "&& #saveFileEvent.getOldFileName() != null " +
            "&& #saveFileEvent.getNewFileName() != null " +
            "&& #saveFileEvent.getOldFileName() != #saveFileEvent.getNewFileName()")
    public void renameAvatar(SaveFileEvent saveFileEvent) {
        if (!saveFileEvent.getNewFileName().isBlank() && !saveFileEvent.getOldFileName().isBlank()) {
            try {
                minioClient.copyObject(CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(saveFileEvent.getNewFileName())
                        .source(
                                CopySource.builder()
                                        .bucket(bucketName)
                                        .object(saveFileEvent.getOldFileName())
                                        .build()
                        )
                        .build());
                minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(saveFileEvent.oldFileName).build());
            } catch (ErrorResponseException
                    | InsufficientDataException
                    | InternalException
                    | InvalidKeyException
                    | InvalidResponseException
                    | IOException
                    | NoSuchAlgorithmException
                    | ServerException
                    | XmlParserException e) {
                throw new StorageException("Could not rename file: " + saveFileEvent.getOldFileName(), e);
            }
        }
    }

    /**
     * Event to delete a avatar belong to Employee ,Student or Workplace
     * condition fileName not null
     * @param deleteFileEvent event
     */
    @EventListener(condition = "#deleteFileEvent.getFileName() != null")
    public void deleteAvatar(DeleteFileEvent deleteFileEvent) {
        if (!deleteFileEvent.getFileName().isBlank()) {
            delete(deleteFileEvent.getFileName().toLowerCase());
        }
    }

    /**
     * Class to storage a File as event
     */
    @Data
    public static class SaveFileEvent {
        private String oldFileName;
        private String newFileName;
        private MultipartFile fileInput;

        SaveFileEvent(String oldFileName, String newFileName, MultipartFile fileInput) {
            this.oldFileName = oldFileName;
            this.newFileName = newFileName;
            this.fileInput = fileInput;
        }

        public static SaveFileEventBuilder builder() {
            return new SaveFileEventBuilder();
        }

        public static class SaveFileEventBuilder {
            private String oldFileName;
            private String newFileName;
            private MultipartFile fileInput;

            SaveFileEventBuilder() {
            }

            public SaveFileEventBuilder oldFileName(String oldFileName) {
                this.oldFileName = oldFileName == null ? "" : oldFileName;
                return this;
            }

            public SaveFileEventBuilder newFileName(String newFileName) {
                this.newFileName = newFileName == null ? "" : newFileName;
                return this;
            }

            public SaveFileEventBuilder fileInput(MultipartFile fileInput) {
                this.fileInput = fileInput;
                return this;
            }

            public SaveFileEvent build() {
                return new SaveFileEvent(oldFileName, newFileName, fileInput);
            }

            public String toString() {
                return "FileService.SaveFileEvent.SaveFileEventBuilder(oldFileName=" + this.oldFileName + ", newFileName=" + this.newFileName + ", fileInput=" + this.fileInput + ")";
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class DeleteFileEvent {
        private String fileName;

        public static DeleteFileEventBuilder builder() {
            return new DeleteFileEventBuilder();
        }

        public static class DeleteFileEventBuilder {
            private String fileName;

            DeleteFileEventBuilder() {
            }

            public DeleteFileEventBuilder fileName(String fileName) {
                this.fileName = fileName;
                return this;
            }

            public DeleteFileEvent build() {
                return new DeleteFileEvent(fileName);
            }

            public String toString() {
                return "FileService.DeleteFileEvent.DeleteFileEventBuilder(fileName=" + this.fileName + ")";
            }
        }
    }
}
