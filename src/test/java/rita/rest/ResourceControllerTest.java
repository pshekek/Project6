package rita.rest;

import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import rita.TestSecurityConfig;
import rita.dto.MessageDto;
import rita.dto.ResourceResponseDto;
import rita.repository.Type;
import rita.service.MinioService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithUserDetails(value = "testuser", userDetailsServiceBeanName = "testUserDetailsService")
@Import(TestSecurityConfig.class)
class ResourceControllerTest {

    private static final String RESOURCE_API = "/api/resource";

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Autowired
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
        byte[] content = "test file content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenAnswer(invocation -> inputStream);

        final byte[] msg = "Hello World".getBytes();


        mockMvc.perform(get("/api/resource")
                        .param("path", "folder/file.txt"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }





    @Test
    void uploadResource_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "object", "file.txt", "text/plain", "test file".getBytes());


        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(createNoSuchKeyException());

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "folder/")
                )
                .andExpect(status().isCreated());
        verify(minioClient, times(1)).putObject(any());
    }

    @Test
    void uploadResource_conflict() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "object",
                "file.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "test file".getBytes()
        );

        StatObjectResponse mockStat = mock(StatObjectResponse.class);

        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(mockStat);

        mockMvc.perform(
                        multipart("/api/resource")
                                .file(file)
                                .param("path", "folder/")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Файл с таким именем уже существует"));

    }


    @Test
    void getInfo_success_is_folder() throws Exception {
        String path = "folder/subfolder/";
        String filename = "subfolder";

        StatObjectResponse mockStat = mock(StatObjectResponse.class);

        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(mockStat);

        mockMvc.perform(
                        get(RESOURCE_API)
                                .param("path", path)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DIRECTORY"))
                .andExpect(jsonPath("$.name").value("subfolder/"));
    }

    @Test
    void getInfo_success_is_file() throws Exception {
        String path = "folder/file.txt";
        String filename = "file.txt";

        StatObjectResponse mockStat = mock(StatObjectResponse.class);

        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(mockStat);

        mockMvc.perform(
                        get(RESOURCE_API)
                                .param("path", path)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FILE"))
                .andExpect(jsonPath("$.name").value("file.txt"));
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

    private io.minio.errors.ErrorResponseException createNoSuchKeyException() {
        io.minio.messages.ErrorResponse errorResponse = new io.minio.messages.ErrorResponse(
                "NoSuchKey",
                "The specified key does not exist.",
                "bucketName",
                null, null, null, null
        );
        return new io.minio.errors.ErrorResponseException(errorResponse, null, null);
    }

}