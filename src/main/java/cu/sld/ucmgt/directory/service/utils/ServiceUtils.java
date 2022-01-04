package cu.sld.ucmgt.directory.service.utils;

import cu.sld.ucmgt.directory.domain.Person;
import org.springframework.web.multipart.MultipartFile;

public final class ServiceUtils {

    /**
     * Build Avatar name using ContentType of MultipartFile (image/png or image/jpeg)
     * @param avatar image
     * @param fileName name of avatar
     * @return image with extension (avatar.png)
     */
    public static String getAvatarNameWithExtension(MultipartFile avatar, String fileName) {
        if (fileName == null || avatar.getContentType() == null) {
            return avatar.getOriginalFilename();
        }
        if (fileName.isBlank() || avatar.getContentType().isBlank()) {
            return avatar.getOriginalFilename();
        }
        String[] extensions = avatar.getContentType().split("/");
        if (extensions.length != 2) {
            return fileName;
        }
        return fileName + "." + extensions[1];
    }

    /**
     * Build Avatar name using ContentType of MultipartFile (image/png or image/jpeg)
     * @param person {@link Person} entity
     * @return image with extension (avatar.png)
     */
    public static String buildAvatarName(Person person) {
        String fileName = person.getName().replaceAll("[^a-zA-Z0-9_-]","").toLowerCase();
        fileName = fileName + "@" + person.getId().toString();
        return fileName;
    }
}
