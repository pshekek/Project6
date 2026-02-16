package rita.service;


import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import io.minio.messages.Item;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import rita.dto.DirectoryResponseDto;
import rita.dto.MessageDto;
import rita.security.AuthenticationHelperImpl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
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
    @SneakyThrows
    @DisplayName("Test get resource info functionality")
    public void givenResourceInfo_whenGetInfo_thenMinioClientIsCalled() {
        //given
        String clientPath = "folder/";
        Long testUserId = 1L;
        DirectoryResponseDto dto = new DirectoryResponseDto(
                "user-1-files/folder/",
                "folder/",
                DIRECTORY);

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);

        given(testMinioClient.statObject(any(StatObjectArgs.class)))
                .willReturn(Mockito.mock(StatObjectResponse.class));

        //when
        ResponseEntity<?> response = testMinioService.getInfo(clientPath);
        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);

    }

    @Test
    @SneakyThrows
    @DisplayName("Test get resource info functionality")
    public void givenResourceInfo_whenGetInfo_thenReturnNotFound() {
        //given
        String clientPath = "folder/";
        Long testUserId = 1L;

        MessageDto dto = new MessageDto("Ресурс не найден");

        ErrorResponse mockErrorResponse = Mockito.mock(ErrorResponse.class);
        given(mockErrorResponse.code()).willReturn("NoSuchKey");
        ErrorResponseException noSuchKeyException = new ErrorResponseException(
                mockErrorResponse,
                null,
                "No such key message"
        );

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);

        given(testMinioClient.statObject(any(StatObjectArgs.class)))
                .willThrow(noSuchKeyException);

        //when
        ResponseEntity<?> response = testMinioService.getInfo(clientPath);
        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .usingRecursiveComparison()
                .isEqualTo(dto);

        verify(testMinioClient, never()).getObject(any(GetObjectArgs.class));

    }


    @Test
    @DisplayName("Test delete resource functionality")
    public void givenStatusNoContent_whenDeleteResource_thenMinioClientIsCalled() {
        //given
        String clientPath = "folder/";
        Long testUserId = 1L;
        Item mockItem = Mockito.mock(Item.class);
        given(mockItem.objectName()).willReturn("user-1-files/folder/");

        Result<Item> result = new Result<>(mockItem);

        Iterable<Result<Item>> results = List.of(result);

        given(testMinioClient.listObjects(any(ListObjectsArgs.class)))
                .willReturn(results);

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);
        //when
        ResponseEntity<?> response = testMinioService.deleteResource(clientPath);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    }

    @Test
    @DisplayName("Test delete resource functionality if resource not found")
    public void givenStatusNotFound_whenDeleteResource_thenReturnNotFound() {
        //given
        String clientPath = "folder/";
        Long testUserId = 1L;

        given(testMinioClient.listObjects(any(ListObjectsArgs.class)))
                .willReturn(Collections.emptyList());

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);
        //when
        ResponseEntity<?> response = testMinioService.deleteResource(clientPath);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    @Test
    @SneakyThrows
    @DisplayName("Test download file functionality")
    public void givenFileAndDownload_whenDownloadResource_thenMinioClientIsCalled() {
        //given
        String clientPath = "file.txt";
        Long testUserId = 1L;
        byte[] content = "писька".getBytes(StandardCharsets.UTF_8);

        GetObjectResponse response = new GetObjectResponse(
                null, null, null, null,
                new ByteArrayInputStream(content)
        );

        given(testMinioClient.getObject(any(GetObjectArgs.class)))
                .willReturn(response);

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);
        //when
        ResponseEntity<?> responseEntity = testMinioService.downloadResource(clientPath);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        String contentDisposition = responseEntity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(contentDisposition).contains("filename=\"file.txt\"");
        InputStreamResource body = (InputStreamResource) responseEntity.getBody();
        assertThat(body).isNotNull();

        byte[] actualContent = body.getInputStream().readAllBytes();
        assertThat(actualContent).isEqualTo(content);

        verify(testMinioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    @SneakyThrows
    @DisplayName("Test download zip archive functionality")
    public void givenZipAndDownload_whenDownloadResource_thenMinioClientIsCalled() {
        //given
        String clientPath = "folder/";
        Long testUserId = 1L;

        Item item1 = mock(Item.class);
        Item item2 = mock(Item.class);

        given(item1.objectName()).willReturn("user1/folder/file1.txt");
        given(item2.objectName()).willReturn("user1/folder/file2.txt");

        Result<Item> result1 = mock(Result.class);
        Result<Item> result2 = mock(Result.class);

        given(result1.get()).willReturn(item1);
        given(result2.get()).willReturn(item2);

        List<Result<Item>> results = List.of(result1, result2);

        given(testMinioClient.listObjects(any(ListObjectsArgs.class)))
                .willReturn(results);


        given(testMinioClient.getObject(argThat(args ->
                args != null && args.object().endsWith("file1.txt")
        ))).willReturn(mockResponse("lol"));

        given(testMinioClient.getObject(argThat(args ->
                args != null && args.object().endsWith("file2.txt")
        ))).willReturn(mockResponse("kek"));

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);

        //when
        ResponseEntity<?> response = testMinioService.downloadResource(clientPath);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains(".zip");
        InputStreamResource body = (InputStreamResource) response.getBody();
        assertThat(body).isNotNull();

        InputStreamResource body1 = (InputStreamResource) response.getBody();
        ZipInputStream zipIn = new ZipInputStream(body1.getInputStream());

        Map<String, String> files = new HashMap<>();

        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
            String name = entry.getName();
            String content1 = new String(zipIn.readAllBytes(), StandardCharsets.UTF_8);
            files.put(name, content1);
        }

        assertThat(files).hasSize(2);
        assertThat(files.values()).containsExactlyInAnyOrder("lol", "kek");

        verify(testMinioClient).getObject(argThat(a ->
                a != null && a.object().endsWith("file1.txt")
        ));

        verify(testMinioClient).getObject(argThat(a ->
                a != null && a.object().endsWith("file2.txt")
        ));    }


    @Test
    @DisplayName("Test Show All Files From Folder functionality")
    public void givenCollectionWithResource_whenShowAllFilesFromFolder_thenMinioClientIsCalled() {

    }

    @Test
    @DisplayName("Test upload File functionality")
    public void givenResource_whenUploadFile_thenMinioClientIsCalled() {

    }

    @Test
    @DisplayName("Test move Or Rename Resource functionality")
    public void givenNewResource_whenMoveOrRenameResource_thenMinioClientIsCalled() {

    }

    @SneakyThrows
    @Test
    @DisplayName("Test create Empty Directory functionality")
    public void givenDirectoryDto_whenCreateEmptyDirectory_thenMinioClientIsCalled() {

        //given
        String clientPath = "folder";
        String fullPath = "rootFolder/folder/";
        Long testUserId = 1L;
        DirectoryResponseDto dto = new DirectoryResponseDto(
                "folder/",
                "folder",
                DIRECTORY);

        ErrorResponse mockErrorResponse = Mockito.mock(ErrorResponse.class);
        given(mockErrorResponse.code()).willReturn("NoSuchKey");
        ErrorResponseException noSuchKeyException = new ErrorResponseException(
                mockErrorResponse,
                null,
                "No such key message"
        );

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);

        given(testMinioClient.putObject(any(PutObjectArgs.class)))
                .willReturn(Mockito.mock(ObjectWriteResponse.class));

        given(testMinioClient.statObject(any(StatObjectArgs.class)))
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
    public void givenDirectoryDtoWithExistPath_whenCreateEmptyDirectory_thenReturnConflict() {

        //given
        String clientPath = "folder";
        Long testUserId = 1L;

        MessageDto dto = new MessageDto("Файл с таким именем уже существует");

        given(authenticationHelper.getCurrentUserId())
                .willReturn(testUserId);

        given(testMinioClient.statObject(any(StatObjectArgs.class)))
                .willReturn(Mockito.mock(StatObjectResponse.class));

        //when
        ResponseEntity<?> response = testMinioService.createEmptyDirectory(clientPath);
        //then
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .usingRecursiveComparison()
                .isEqualTo(dto);

        verify(testMinioClient, never()).putObject(any(PutObjectArgs.class));
    }


    @Test
    @DisplayName("Test create Empty Directory functionality")
    public void givenResource_whenSearchResource_thenMinioClientIsCalled() {

    }

    private GetObjectResponse mockResponse(String text) {
        return new GetObjectResponse(
                null, null, null, null,
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))
        );
    }


}
