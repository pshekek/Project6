package rita.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;
import rita.dto.DirectoryResponseDto;
import rita.dto.MessageDto;
import rita.repository.Type;
import rita.repository.UserRepository;
import rita.security.AuthenticationHelper;
import rita.security.AuthenticationHelperImpl;
import rita.service.MinioService;
import rita.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class ItDirectoryRestControllerTest extends AbstractControllerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationHelperImpl authenticationHelper;

    @Autowired
    private MinioClient minioClient;

    @BeforeEach
    void createBucketIfNotExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket("user-files").build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket("user-files").build()
            );
        }
    }

    @AfterEach
    void cleanBucket() throws Exception {

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("user-files")
                            .object(item.objectName())
                            .build()
            );
        }
    }


    @Test
    @DisplayName("Test Create empty Folder functionality")
    public void givenClientPath_whenCreateEmptyDirectory_thenSuccessResponse() throws Exception {


        given(authenticationHelper.getCurrentUserId()).willReturn(1L);


        mockMvc.perform(post("/api/directory")
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .param("path", "path/folder/"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("path/folder/")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.is("folder/")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.is("DIRECTORY")));
    }

    @Test
    @DisplayName("Test Create empty Folder when path already exist functionality")
    public void givenExistingClientPath_whenCreateEmptyDirectory_thenConflictResponse()
            throws Exception {

        given(authenticationHelper.getCurrentUserId()).willReturn(1L);

        mockMvc.perform(post("/api/directory")
                .with(csrf())
                .with(user("testUser").roles("USER"))
                .param("path", "path/folder/"));

        mockMvc.perform(post("/api/directory")
                        .with(csrf())
                        .with(user("testUser").roles("USER"))
                        .param("path", "path/folder/"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message",
                        CoreMatchers.is("Файл с таким именем уже существует")));
    }

}
