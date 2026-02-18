package rita.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rita.dto.DirectoryResponseDto;
import rita.dto.MessageDto;
import rita.dto.ResourceResponseDto;
import rita.exeptions.EntityAlreadyExistsException;
import rita.exeptions.MinioException;
import rita.repository.UserRepository;
import rita.security.AuthenticationHelperImpl;

import javax.persistence.EntityNotFoundException;
import javax.validation.ValidationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static rita.repository.Type.DIRECTORY;
import static rita.repository.Type.FILE;

@Service
@RequiredArgsConstructor
@Log4j2
public class MinioService {

    private final MinioClient minioClient;
    private final NamingService namingService;
    private final AuthenticationHelperImpl authenticationHelper;
    private static final String USER_PREFIX = "user-%d-files/";
    private static final Set<Character> INVALID_CHARS = Set.of(
            '\\', ':', '*', '?', '"', '\'', '<', '>', '|'
    );


    public ResourceResponseDto getInfo(String clientPath) {

        Long userId = authenticationHelper.getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

        StatObjectResponse statObject;

        try {
            statObject = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("user-files")
                            .object(path)
                            .build()
            );
        } catch (io.minio.errors.ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new EntityNotFoundException("Ресурс не найден");
            }
            throw new MinioException("Неизвестная ошибка Minio: " + e.errorResponse().message());
        } catch (Exception e) {
            throw new MinioException("Не удалось получить информацию о ресурсе", e);
        }

        if (path.endsWith("/")) {
            return new ResourceResponseDto(
                    path,
                    namingService.getNameFromPath(clientPath),
                    null,
                    DIRECTORY
            );
        } else {
            long size = statObject.size();
            return new ResourceResponseDto(
                    path,
                    namingService.getNameFromPath(clientPath),
                    size,
                    FILE
            );
        }
    }

    public void deleteResource(String clientPath) {

        Long userId = authenticationHelper.getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .prefix(path)
                        .recursive(true)
                        .build()
        );
        boolean hasFiles = false;

        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                hasFiles = true;

                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket("user-files")
                                .object(item.objectName())
                                .build()
                );
            }
        } catch (Exception e) {
            throw new MinioException("Неизвестная ошибка Minio: " + e.getMessage());
        }

        if (!hasFiles) {
            throw new EntityNotFoundException("Ресурс не найден");
        }
    }


    public InputStreamResource downloadResource(String clientPath) {

        Long userId = authenticationHelper.getCurrentUserId();
        String path = buildFullPath(clientPath, userId);
        InputStreamResource resource;
        try {
            String fileName = namingService.getNameFromPath(path);
            boolean isFolder = path.endsWith("/");

            if (!isFolder)
                try {
                    resource = new InputStreamResource(minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket("user-files")
                                    .object(path)
                                    .build()
                    ));

                } catch (io.minio.errors.ErrorResponseException e) {
                    if ("NoSuchKey".equals(e.errorResponse().code())) {
                        throw new EntityNotFoundException("Ресурс не найден");
                    }
                    throw new MinioException("Неизвестная ошибка Minio: " + e.errorResponse().message());
                } catch (Exception e) {
                    throw new MinioException("Не удалось получить информацию о ресурсе", e);
                }
            else {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream);
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket("user-files")
                                .prefix(path)
                                .recursive(true)
                                .build()
                );
                boolean hasFiles = false;
                for (Result<Item> result : results) {

                    Item item = result.get();
                    if (item.objectName().equals(path)) {
                        continue;
                    }
                    hasFiles = true;

                    try (InputStream inputStream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket("user-files")
                                    .object(item.objectName())
                                    .build()
                    )) {
                        String archiveName = namingService.getParentFolder(path) + namingService.getNameFromPath(item.objectName());
                        ZipEntry zipEntry = new ZipEntry(archiveName);
                        zipOut.putNextEntry(zipEntry);
                        inputStream.transferTo(zipOut);
                        zipOut.closeEntry();
                    }
                }
                if (!hasFiles) {
                    ZipEntry emptyDir = new ZipEntry(fileName + "/");
                    zipOut.putNextEntry(emptyDir);
                    zipOut.closeEntry();
                }
                zipOut.close();

                resource = new InputStreamResource
                        (new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

            }
        } catch (Exception e) {
            throw new MinioException("Неизвестная ошибка Minio: " + e.getMessage());
        }
        return resource;
    }


    public List<ResourceResponseDto> showAllFilesFromFolder(String clientPath) {

        Long userId = authenticationHelper.getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

        if (!clientPath.isEmpty() && !isFolderExists(path)) {
            throw new EntityNotFoundException("Ресурс не найден");
        }

        List<ResourceResponseDto> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket("user-files")
                            .prefix(path)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                if (objectName.equals(path)) {
                    continue;
                }
                String fileName = namingService.getNameFromPath(objectName);
                files.add(new ResourceResponseDto(namingService.getParentFolder(path), fileName, item.size(),
                        item.objectName().endsWith("/") ? DIRECTORY : FILE));
            }
        } catch (Exception e) {
            throw new MinioException("Неизвестная ошибка Minio: " + e.getMessage());
        }
        return files;
    }


    public List<ResourceResponseDto> uploadFile(List<MultipartFile> multipartFiles, String clientPath) {

        Long userId = authenticationHelper.getCurrentUserId();
        List<ResourceResponseDto> files = new ArrayList<>();

        for (MultipartFile multipartFile : multipartFiles) {
            String fileName = multipartFile.getOriginalFilename();
            validateName(namingService.getNameFromPath(fileName));
            String fullClientPath = clientPath + fileName;
            String path = buildFullPath(fullClientPath, userId);
            boolean exists = false;
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket("user-files")
                                .object(path)
                                .build()
                );
                exists = true;

            } catch (io.minio.errors.ErrorResponseException e) {

                if (!"NoSuchKey".equals(e.errorResponse().code())) {
                    throw new MinioException(
                            "Неизвестная ошибка Minio: " + e.errorResponse().message());
                }

            } catch (Exception e) {
                throw new MinioException(
                        "Не удалось получить информацию о ресурсе", e);
            }
            if (exists) {
                throw new EntityAlreadyExistsException("Файл с таким именем уже существует");
            }
            try (InputStream inputStream = multipartFile.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket("user-files")
                                .object(path)
                                .stream(inputStream, multipartFile.getSize(), -1)
                                .contentType(multipartFile.getContentType())
                                .build());

            } catch (Exception e) {
                throw new MinioException("Неизвестная ошибка Minio: ", e);
            }

            String displayName = namingService.getNameFromPath(fileName);
            files.add(new ResourceResponseDto(
                    namingService.getParentFolder(path),
                    displayName,
                    multipartFile.getSize(),
                    multipartFile.getOriginalFilename().endsWith("/") ? DIRECTORY : FILE
            ));
        }
        return files;
    }


    public ResourceResponseDto moveOrRenameResource(String fromClient, String toClient) {
        Long userId = authenticationHelper.getCurrentUserId();

        String from = buildFullPath(fromClient, userId);
        String to = buildFullPath(toClient, userId);

        validateName(namingService.getNameFromPath(toClient));

        boolean notExist = isNotExist(to);

        if (!notExist) {
            throw new EntityAlreadyExistsException("Файл с таким именем уже существует");
        }

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket("user-files")
                            .prefix(from)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                String oldObject = item.objectName();
                String relative = oldObject.substring(from.length());
                String newObject = to + relative;

                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket("user-files")
                                .object(newObject)
                                .source(CopySource.builder()
                                        .bucket("user-files")
                                        .object(oldObject)
                                        .build())
                                .build()
                );

            }
            for (Result<Item> result : minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket("user-files")
                            .prefix(from)
                            .recursive(true)
                            .build()
            )) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket("user-files")
                        .object(result.get().objectName())
                        .build()
                );
            }


            if (from.endsWith("/")) {
                return new ResourceResponseDto(
                        namingService.getParentFolder(to),
                        namingService.getNameFromPath(to),
                        null,
                        DIRECTORY);
            } else {
                StatObjectResponse info = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket("user-files")
                                .object(to)
                                .build()
                );
                return new ResourceResponseDto(
                        namingService.getParentFolder(to),
                        namingService.getNameFromPath(to),
                        info.size(),
                        FILE);
            }

        } catch (Exception e) {
            throw new MinioException("Неизвестная ошибка Minio: ", e);
        }
    }

    public ResourceResponseDto createEmptyDirectory(String clientPath) {
        Long userId = authenticationHelper.getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

        validateName(namingService.getNameFromPath(clientPath));
        boolean notExist = isNotExist(path);
        if (!notExist) {
            throw new EntityAlreadyExistsException("Файл с таким именем уже существует");
        }

        try {
                InputStream emptyFolder = new ByteArrayInputStream(new byte[]{});
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket("user-files")
                                .object(path)
                                .stream(emptyFolder, 0, 0)
                                .build()
                );

                String displayName = namingService.getNameFromPath(path);
                String rootFolder = namingService.getParentFolder(path);
                return new ResourceResponseDto(
                                rootFolder,
                                displayName,
                                null,
                                DIRECTORY
                        );

        } catch (Exception e) {
            throw new MinioException("Неизвестная ошибка Minio: ", e);
        }
    }

    public List<ResourceResponseDto> searchResource(String query) {
        Long userId = authenticationHelper.getCurrentUserId();
        String path = buildFullPath("", userId);
        validateName(query);
        query = query.toLowerCase();
        List<ResourceResponseDto> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket("user-files")
                            .prefix(path)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {

                if (result.get().objectName().toLowerCase().contains(query)) {
                    String parentFolder = namingService.getParentFolder(result.get().objectName());
                    if (parentFolder.equals(namingService.getNameFromPath(result.get().objectName()))) {
                        parentFolder = namingService.getNameFromPath(result.get().objectName()).replaceAll("[^/]", "");
                    }
                    files.add(new ResourceResponseDto(
                            parentFolder,
                            namingService.getNameFromPath(result.get().objectName()),
                            result.get().size(),
                            result.get().objectName().endsWith("/") ? DIRECTORY : FILE
                    ));
                }
            }
        } catch (Exception e) {
            throw new MinioException("Неизвестная ошибка Minio: ", e);
        }
        return files;
    }


    private String prefix(Long userId) {
        return USER_PREFIX.formatted(userId);
    }


    private String buildFullPath(String clientPath, Long userId) {
        if (clientPath.startsWith("/")) {
            clientPath = clientPath.substring(1);
        }
        return prefix(userId) + clientPath;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Имя не может быть пустым");
        }

        boolean isDirectory = name.endsWith("/");
        String clean = isDirectory ? name.substring(0, name.length() - 1) : name;

        if (clean.isBlank()) {
            throw new ValidationException("Недопустимое имя");
        }

        if (name.contains("//")) {
            throw new ValidationException("Недопустимо использовать //");
        }

        if (name.contains("..")) {
            throw new ValidationException("Недопустимо использовать ..");
        }

        for (char c : clean.toCharArray()) {
            if (INVALID_CHARS.contains(c)) {
                throw new ValidationException("Недопустимый символ: " + c);
            }
            if (Character.isISOControl(c)) {
                throw new ValidationException("Недопустимый управляющий символ");
            }
        }
    }

    private boolean isNotExist(String path) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("user-files")
                            .object(path)
                            .build()
            );
            return false;

        } catch (io.minio.errors.ErrorResponseException ex) {

            if ("NoSuchKey".equals(ex.errorResponse().code())) {
                return true;
            }

            throw new MinioException(
                    "Ошибка Minio: " + ex.errorResponse().message(), ex);

        } catch (Exception e) {
            throw new MinioException("Ошибка проверки существования объекта", e);
        }
    }

    boolean isFolderExists(String path) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket("user-files")
                            .prefix(path)
                            .build()
            );
            boolean isFolder = path.endsWith("/");
            for (Result<Item> result : results) {
                Item item = result.get();
                String name = item.objectName();
                if (isFolder) {
                    if (name.startsWith(path)) {
                        return true;
                    } else return false;
                } else {
                    if (namingService.getParentFolder(name).equals(namingService.getParentFolder(path))) {
                        return true;
                    }
                }
            }
        } catch (
                Exception e) {
            return false;
        }
        return false;
    }
}


