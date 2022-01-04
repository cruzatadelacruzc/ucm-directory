package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.service.FileService;
import cu.sld.ucmgt.directory.web.rest.util.HeaderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileResource {

    @Value("${application.clientApp.name}")
    private String applicationName;
    private final FileService service;
    private static final String ENTITY_NAME = "File";

    /**
     * {@code POST  /upload} : upload file.
     * For more information about this procedure "upload image" see https://stackoverflow.com/a/49993072
     *
     * @param file to save.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body url of file.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) throws URISyntaxException {
        String fileId = service.store(file, null);
        return ResponseEntity.created(new URI("/api/files/" + fileId))
                .headers(HeaderUtil.createEntityExecutedAlert(applicationName,true, ENTITY_NAME, fileId))
                .body(fileId);
    }

    /**
     * {@code GET /files/:avatar} : get the "avatar" file.
     *
     * @param avatar the name file to retrieve
     * @return the {@link ResponseEntity} with status {@code 200 OK} and with body the Resource
     */
    @GetMapping(value = "/{avatar:.+}", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<Resource> getAvatar(@PathVariable String avatar, HttpServletRequest request) {
        Optional<Resource> optionalResource = service.loadAsResource(avatar);
        HttpHeaders headers = new HttpHeaders();
        optionalResource.ifPresent(resource -> headers.setContentType(MediaType.parseMediaType(getContentType(resource, request))));

        return optionalResource.map(resource -> ResponseEntity.ok()
                .headers(headers)
                .body(resource))
        .orElse(ResponseEntity.notFound().build());
    }

    /**
     * {@code DELETE  /files/:filename} : delete the "filename" file.
     * @param fileName name of file
     * @return True if file was deleted False otherwise
     */
    public ResponseEntity<Boolean> deleteFile(@PathVariable(name = "filename") String fileName) {
        boolean result = service.delete(fileName);
        return ResponseEntity.ok(result);
    }

    private String getContentType(Resource resource, HttpServletRequest request) {
        String contentType = null;
        try {
            contentType  = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        }catch(IOException ex) {
            log.warn("Could not determine fileType");
        }
        return contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
    }
}
