package cu.sld.ucmgt.directory.web.rest;

import cu.sld.ucmgt.directory.DirectoryApp;
import cu.sld.ucmgt.directory.config.TestSecurityConfiguration;
import cu.sld.ucmgt.directory.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@link FileResource} REST controller.
 */
@WithMockUser
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {DirectoryApp.class, TestSecurityConfiguration.class})
public class FileResourceIT {

    @Autowired
    private MockMvc restMockMvc;
    @Autowired
    private FileService service;
    MockMultipartFile multipartFile;

    @Test
    public void shouldSaveUploadedFile() throws Exception {
        multipartFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "Spring Framework".getBytes());
        MvcResult fileResult = restMockMvc.perform(multipart("/api/files/upload").file(multipartFile))
                .andExpect(status().isCreated())
                .andReturn();

        String location = fileResult.getResponse().getHeader("Location");
        String fileId = fileResult.getResponse().getContentAsString();
        assertThat(location).isNotNull();
        assertThat(fileId).isNotNull();
        assertThat(location).isEqualTo("/api/files/" + fileId);
    }

    @Test
    public void shouldDeleteUploadedFile() throws Exception {
        String filename = "test.txt";
        multipartFile = new MockMultipartFile("file", filename,
                "text/plain", "Spring Framework".getBytes());
        service.store(multipartFile,filename);

        MvcResult fileResult = restMockMvc.perform(delete("/api/files/{filename}", filename))
                .andExpect(status().isOk())
                .andReturn();

        String response = fileResult.getResponse().getContentAsString();
        assertThat(response).isEqualTo("true");
    }

    @Test
    public void getAvatar() throws Exception {
        System.out.println("not implement yet");
    }
}
