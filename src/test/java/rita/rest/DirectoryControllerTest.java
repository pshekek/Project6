package rita.rest;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import rita.TestSecurityConfig;
import rita.dto.DirectoryResponseDto;
import rita.dto.MessageDto;
import rita.dto.ResourceResponseDto;
import rita.repository.Type;
import rita.service.MinioService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithUserDetails(value = "testuser", userDetailsServiceBeanName = "testUserDetailsService")
@Import(TestSecurityConfig.class)
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

    private final DirectoryResponseDto dir = new DirectoryResponseDto(
            "user-1-files/", "folder/", Type.DIRECTORY
    );
    private final MessageDto pathNotFound = new MessageDto("Папки не существует");


    @Test
    void showAllFilesFromFolder_Success() throws Exception {
        final String testPath = "documents/";

        ResourceResponseDto file = new ResourceResponseDto(
                testPath, "file.txt", 1024L, Type.FILE
        );
        ResponseEntity<Object> successResponse = ResponseEntity.ok(java.util.List.of(file));

        doReturn(successResponse)
                .when(minioService)
                .showAllFilesFromFolder(anyString());


        mockMvc.perform(
                        get(DIRECTORY_API)
                                .param("path", testPath)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                // 4. Проверка статуса и содержимого
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("file.txt"))
                .andExpect(jsonPath("$[0].size").value(1024L));


        verify(minioService, times(1)).showAllFilesFromFolder(testPath);
    }

    @Test
    void showAllFilesFromFolder_NotFound() throws Exception {
        final String testPath = "non_existent_folder/";

        ResponseEntity<Object> notFoundResponse = ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(pathNotFound);

        doReturn(notFoundResponse)
                .when(minioService)
                .showAllFilesFromFolder(anyString());


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

        doReturn(ResponseEntity.badRequest().body(new MessageDto("Невалидный или отсутствующий путь")))
                .when(minioService)
                .showAllFilesFromFolder(anyString());

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

        ResponseEntity<Object> createdResponse = ResponseEntity
                .status(HttpStatus.CREATED)
                .body(dir);

        doReturn(createdResponse)
                .when(minioService)
                .createEmptyDirectory(newPath);

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

        MessageDto conflictMessage = new MessageDto("Файл с таким именем уже существует");
        ResponseEntity<Object> conflictResponse = ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(conflictMessage);

        doReturn(conflictResponse)
                .when(minioService)
                .createEmptyDirectory(existingPath);

        mockMvc.perform(
                        post(DIRECTORY_API)
                                .param("path", existingPath)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Файл с таким именем уже существует"));

        verify(minioService, times(1)).createEmptyDirectory(existingPath);
    }
}