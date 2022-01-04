package cu.sld.ucmgt.directory.service;

import cu.sld.ucmgt.directory.config.AppProperties;
import cu.sld.ucmgt.directory.service.error.StorageException;
import cu.sld.ucmgt.directory.service.error.StorageFileNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;
import java.util.Optional;

/**
 * Storage service interface
 */
@Slf4j
@Service
public class FileService {

    private final Path rootLocation;

    public FileService(AppProperties appProperties) {
        this.rootLocation = Paths.get(appProperties.getStorage().getUploadDir()).toAbsolutePath().normalize();
    }

    /**
     *  To create files directory
     */
    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(this.rootLocation)) {
                Path path = Files.createDirectories(this.rootLocation);
                log.info("Create directory at the path : " + path.normalize().toString());
            } else {
                log.info("A directory already exists at the path : " + this.rootLocation.normalize().toString());
            }
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
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
                    Files.copy(inputStream, this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                }
                return filename;
            } catch (IOException e) {
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
        boolean result = false;
        try {
            result = Files.deleteIfExists(rootLocation.resolve(filename));
        } catch (NoSuchFileException e) {
            log.warn("No such file exists with name: " + filename);
        } catch (IOException exception) {
            log.error("Could not delete file", exception);
        }
        return result;
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
                File fileToMove = rootLocation.resolve(saveFileEvent.getOldFileName()).toFile();
                String extension = FilenameUtils.getExtension(fileToMove.getName());
                String target = rootLocation.resolve(saveFileEvent.getNewFileName()).normalize().toString();
                if (extension.isBlank()) {
                    target = rootLocation.resolve(saveFileEvent.getNewFileName()).normalize().toString() + "." + extension;
                }
                boolean isMoved = fileToMove.renameTo(new File(target));
                if (!isMoved) {
                    throw new FileSystemException(target);
                }
            } catch (SecurityException | FileSystemException exception) {
                throw new StorageException("Could not rename file: " + saveFileEvent.getOldFileName(), exception);
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
     * Load the stored filename
     * @param filename Resource name
     * @return Resource The stored resource
     */
    public Optional<Resource> loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return Optional.of(resource);
            }
            return Optional.empty();
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    /**
     * Load the stored filename
     * @param filename Resource name
     * @return byte Array The stored resource
     */
    public byte[] loadAsBytes(String filename) {
        try {
            FileInputStream inputStream = new FileInputStream(rootLocation.resolve(filename).normalize().toString());
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("Could not read file: " + filename, e);
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
