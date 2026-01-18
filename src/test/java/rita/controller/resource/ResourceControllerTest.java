package rita.controller.resource;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import rita.dto.BaseResponse;
import rita.dto.MessageDto;
import rita.dto.ResourceResponseDto;
import rita.repository.Type;
import rita.service.MinioService;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithUserDetails(value = "testuser", userDetailsServiceBeanName = "testUserDetailsService")
@Import(TestSecurityConfig.class)
class ResourceControllerTest {

    private static final String RESOURCE_API = "/api/resource";

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

    private final ResourceResponseDto resource = new ResourceResponseDto(
            "user-1-files/", "folder/1", 12L, Type.FILE
    );
    private final MessageDto pathNotFound = new MessageDto("Русурс не существует");


    @Test
    void downloadResource_success() throws Exception {
        final String path = "folder/1/file.txt";
        byte[] content = "test file content".getBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "file.txt");
        ResponseEntity<Object> successResponse =
                new ResponseEntity<>(content, headers, HttpStatus.OK);

        doReturn(successResponse)
                .when(minioService)
                .downloadResource(path);
        mockMvc.perform(
                        get(RESOURCE_API)
                                .param("path", path)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"file.txt\""
                ))
                .andExpect(content().bytes(content));
        verify(minioService, times(1)).downloadResource(path);
    }

/*    @Test
    void downloadResource_InvalidPath_BadRequest() throws Exception {
        final String invalidPath = "///";

        ResponseEntity<Object> badRequestResponse = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageDto("Невалидный или отсутствующий путь"));

        doReturn(badRequestResponse)
                .when(minioService)
                .downloadResource(invalidPath);

        mockMvc.perform(
                        get(RESOURCE_API)
                                .param("path", invalidPath)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Невалидный или отсутствующий путь"));

        verify(minioService, times(1)).downloadResource(invalidPath);
    }*/

    @Test
    void uploadResource_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "object", "file.txt", "text/plain", "test file".getBytes());

        ResponseEntity<Object> createdResponse =
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(List.of());

        doReturn(createdResponse)
                .when(minioService)
                .uploadFile(anyList(), anyString());

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "folder/")
                )
                .andExpect(status().isCreated());
    }

    @Test
    void uploadResource_conflict() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "object",
                "file.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "test file".getBytes()
        );

        ResponseEntity<Object> conflictResponse =
                ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new MessageDto("Файл с таким именем уже существует"));

        doReturn(conflictResponse)
                .when(minioService)
                .uploadFile(anyList(), anyString());

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "folder/")
                )
                .andExpect(status().isConflict());
    }


    @Test
    void getInfo() {
    }

    @Test
    void deleteResource() {
    }

    @Test
    void moveResource() {
    }

    @Test
    void searchResource() {
    }
}