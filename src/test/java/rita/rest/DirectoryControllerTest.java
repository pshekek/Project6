package rita.rest;

import io.minio.MinioClient;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import rita.dto.ResourceResponseDto;
import rita.exeptions.EntityAlreadyExistsException;
import rita.repository.Type;
import rita.service.MinioService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithUserDetails(value = "testuser", userDetailsServiceBeanName = "testUserDetailsService")
@Import(rita.config.TestSecurityConfig.class)
class DirectoryControllerTest {

    private static final String DIRECTORY_API = "/api/directory";

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @MockBean
    private MinioService minioService;

    @MockBean
    private MinioClient minioClient;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
            .apply(springSecurity())
            .build();
    }

    @Test
    void showAllFilesFromFolder_Success() throws Exception {
        final String testPath = "documents/";

        ResourceResponseDto file = new ResourceResponseDto(
            testPath, "file.txt", 1024L, Type.FILE
        );

        // Исправлено: сервис возвращает List<ResourceResponseDto>, а не ResponseEntity
        when(minioService.showAllFilesFromFolder(testPath)).thenReturn(List.of(file));

        mockMvc.perform(
                get(DIRECTORY_API)
                    .param("path", testPath)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].name").value("file.txt"))
            .andExpect(jsonPath("$[0].size").value(1024L));

        verify(minioService, times(1)).showAllFilesFromFolder(testPath);
    }

    @Test
    void showAllFilesFromFolder_NotFound() throws Exception {
        final String testPath = "non_existent_folder/";

        // Исправлено: сервис выбрасывает исключение
        when(minioService.showAllFilesFromFolder(testPath))
            .thenThrow(new EntityNotFoundException("Папки не существует"));

        mockMvc.perform(
                get(DIRECTORY_API)
                    .param("path", testPath)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Папки не существует"));

        verify(minioService, times(1)).showAllFilesFromFolder(testPath);
    }

    @Test
    void showAllFilesFromFolder_NoPath() throws Exception {

        mockMvc.perform(
                get(DIRECTORY_API)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest());

        verify(minioService, never()).showAllFilesFromFolder(anyString());
    }

    @Test
    void createEmptyDirectory_Success() throws Exception {
        final String newPath = "new_folder/";

        ResourceResponseDto dir = new ResourceResponseDto(
            "user-1-files/", "folder/", null, Type.DIRECTORY
        );

        when(minioService.createEmptyDirectory(newPath)).thenReturn(dir);

        mockMvc.perform(
                post(DIRECTORY_API)
                    .param("path", newPath)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(dir.getName()));

        verify(minioService, times(1)).createEmptyDirectory(newPath);
    }

    @Test
    void createEmptyDirectory_Conflict() throws Exception {
        final String existingPath = "documents/";

        when(minioService.createEmptyDirectory(existingPath))
            .thenThrow(new EntityAlreadyExistsException("Папка с таким именем уже существует"));

        mockMvc.perform(
                post(DIRECTORY_API)
                    .param("path", existingPath)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Папка с таким именем уже существует"));

        verify(minioService, times(1)).createEmptyDirectory(existingPath);
    }
}