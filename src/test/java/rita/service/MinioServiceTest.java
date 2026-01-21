package rita.service;


import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import lombok.SneakyThrows;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import rita.dto.DirectoryResponseDto;
import rita.dto.MessageDto;
import rita.security.AuthenticationHelperImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static rita.repository.Type.DIRECTORY;

@ExtendWith(MockitoExtension.class)
public class MinioServiceTest {

    @Mock
    private MinioClient testMinioClient;

    @Mock
    private AuthenticationHelperImpl authenticationHelper;

    @InjectMocks
    private MinioService testMinioService;


    @Test
    @DisplayName("Test get resource info functionality")
    public void givenResourceInfo_whenGetInfo_thenMinioClientIsCalled () {


    }

    @Test
    @DisplayName("Test delete resource functionality")
    public void givenStatusNoContent_whenDeleteResource_thenMinioClientIsCalled () {

    }
    @Test
    @DisplayName("Test download resource functionality")
    public void givenResourceAndDownload_whenDownloadResource_thenMinioClientIsCalled () {

    }
    @Test
    @DisplayName("Test Show All Files From Folder functionality")
    public void givenCollectionWithResource_whenShowAllFilesFromFolder_thenMinioClientIsCalled () {

    }
    @Test
    @DisplayName("Test upload File functionality")
    public void givenResource_whenUploadFile_thenMinioClientIsCalled () {

    }

    @Test
    @DisplayName("Test move Or Rename Resource functionality")
    public void givenNewResource_whenMoveOrRenameResource_thenMinioClientIsCalled () {

    }

    @SneakyThrows
    @Test
    @DisplayName("Test create Empty Directory functionality")
    public void givenDirectoryDto_whenCreateEmptyDirectory_thenMinioClientIsCalled () {

        //given
        String clientPath = "folder";
        String fullPath = "rootFolder/folder/";
        Long testUserId = 1L;
        DirectoryResponseDto dto = new DirectoryResponseDto(
                "folder/",
                "folder",
                DIRECTORY);

        ErrorResponse mockErrorResponse = Mockito.mock(ErrorResponse.class);
        BDDMockito.given(mockErrorResponse.code()).willReturn("NoSuchKey");
        ErrorResponseException noSuchKeyException = new ErrorResponseException(
                mockErrorResponse,
                null,
                "No such key message"
        );

        BDDMockito.given(authenticationHelper.getCurrentUserId())
                        .willReturn(testUserId);

        BDDMockito.given(testMinioClient.putObject(any(PutObjectArgs.class)))
                .willReturn(Mockito.mock(ObjectWriteResponse.class));

        BDDMockito.given(testMinioClient.statObject(any(StatObjectArgs.class)))
                .willThrow(noSuchKeyException);

        //when

         ResponseEntity<?> response = testMinioService.createEmptyDirectory(clientPath);
        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);

    }

    @Test
    @SneakyThrows
    @DisplayName("Test create Empty Directory if Directory already exist functionality")
    public void givenDirectoryDtoWithExistPath_whenCreateEmptyDirectory_thenReturnConflict () {

        //given
        String clientPath = "folder";
        Long testUserId = 1L;

        MessageDto dto =  new MessageDto("Файл с таким именем уже существует");

        BDDMockito.given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);

        BDDMockito.given(testMinioClient.statObject(any(StatObjectArgs.class)))
                .willReturn(Mockito.mock(StatObjectResponse.class));

        //when
        ResponseEntity<?> response = testMinioService.createEmptyDirectory(clientPath);
        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .usingRecursiveComparison()
                .isEqualTo(dto);
    }


    @Test
    @DisplayName("Test create Empty Directory functionality")
    public void givenResource_whenSearchResource_thenMinioClientIsCalled () {

    }



}
