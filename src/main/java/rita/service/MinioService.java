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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rita.dto.DirectoryResponseDto;
import rita.dto.ResourceResponseDto;
import rita.exeptions.EntityAlreadyExistsException;
import rita.repository.Resource;
import rita.repository.UserRepository;
import rita.security.MyUserDetails;

import javax.persistence.EntityNotFoundException;
import javax.validation.ValidationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static rita.repository.Type.DIRECTORY;
import static rita.repository.Type.FILE;

@Service
@RequiredArgsConstructor
@Log4j2
public class MinioService {

    private final MinioClient minioClient;
    private final UserRepository userRepository;
    private static final String USER_PREFIX = "user-%d-files/";
    private static final Set<Character> INVALID_CHARS = Set.of(
            '\\', ':', '*', '?', '"', '\'', '<', '>', '|'
    );


    public ResponseEntity<?> getInfo(String clientPath) {

        if (clientPath == null || clientPath.isEmpty()) {
            throw new ValidationException("Отсутствует путь");
        }

        Long userId = getCurrentUserId();
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
                throw new EntityNotFoundException("Ресурс не найден: " + path);
            }
            throw new RuntimeException("Неизвестная ошибка Minio: " + e.errorResponse().message());
        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить информацию о ресурсе", e);
        }

        if (path.endsWith("/")) {
            return ResponseEntity.ok()
                    .body(new DirectoryResponseDto(
                            path,
                            getNameFromPath(clientPath),
                            DIRECTORY
                    ));
        } else {

            long size = statObject.size();
            return ResponseEntity.ok()
                    .body(new ResourceResponseDto(
                            path,
                            getNameFromPath(clientPath),
                            size,
                            FILE
                    ));
        }
    }

    public void deleteResource(String clientPath) {

        if (clientPath == null || clientPath.isEmpty()) {
            throw new ValidationException("Отсутствует путь");
        }

        Long userId = getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket("user-files")
                        .prefix(path)
                        .recursive(true)
                        .build()
        );
        boolean hasFiles = false;
        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                hasFiles = true;
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket("user-files")
                                    .object(item.objectName())
                                    .build()
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Ошибка удаления из minio", e);
                }
            } catch (Exception e) {
                throw new RuntimeException("Ошибка удаления из minio", e);
            }
        }
    }

    public ResponseEntity<?> downloadResource(String clientPath) {

        if (clientPath == null || clientPath.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Невалидный или отсутствующий путь");
        }
        Long userId = getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

        try {
            InputStreamResource resource;
            String fileName = getNameFromPath(path);
            boolean isFolder = path.endsWith("/");

            if (!isFolder)
                try {
                    resource = new InputStreamResource(minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket("user-files")
                                    .object(path)
                                    .build()
                    ));

                } catch (Exception e) {
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body("Ресурс не найден");
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
                        String archiveName = getParentFolder(path) + getNameFromPath(item.objectName());
                        ZipEntry zipEntry = new ZipEntry(archiveName);
                        zipOut.putNextEntry(zipEntry);
                        inputStream.transferTo(zipOut);
                        zipOut.closeEntry();
                    }
                }
                if (!hasFiles) {
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body("Папка пуста или не найдена");
                }
                zipOut.close();

                resource = new InputStreamResource
                        (new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                fileName += ".zip";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Неизвестная ошибка при скачивании файла");
        }
    }


    public ResponseEntity<?> showAllFilesFromFolder(String clientPath) {
        Long userId = getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

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
                String fileName = getNameFromPath(objectName);
                files.add(new ResourceResponseDto(getParentFolder(path), fileName, item.size(),
                        item.objectName().endsWith("/") ? DIRECTORY : FILE));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok()
                .body(files);
    }


    public ResponseEntity<?> uploadFile(MultipartFile multipartFile, String clientPath) {
        Long userId = getCurrentUserId();
        String path = buildFullPath(clientPath, userId);
        String fileName;
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket("user-files")
                            .object(path)
                            .build()
            );}
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        try (InputStream inputStream = multipartFile.getInputStream()) {
            fileName = multipartFile.getOriginalFilename();
            validateName(getNameFromPath(fileName));
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("user-files")
                            .object(path + "/" + fileName)
                            .stream(inputStream, multipartFile.getSize(), -1)
                            .contentType(multipartFile.getContentType())
                            .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        String originalFileName = multipartFile.getOriginalFilename();
        String displayName = getNameFromPath(originalFileName);
        List<ResourceResponseDto> files = List.of(new ResourceResponseDto(
                getParentFolder(path),
                displayName,
                multipartFile.getSize(),
                multipartFile.getName().endsWith("/") ? DIRECTORY : FILE));

        return ResponseEntity.status(HttpStatus.CREATED).body(files);
    }

    public ResponseEntity<?> moveOrRenameResource(String fromClient, String toClient) {
        Long userId = getCurrentUserId();

        String from = buildFullPath(fromClient, userId);
        String to = buildFullPath(toClient, userId);

        validateName(getNameFromPath(toClient));

        if (from.equals(to)) {
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        try {
            InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket("user-files")
                        .object(to)
                        .build()
        );}
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket("user-files")
                            .object(to)
                            .source(CopySource.builder()
                                    .bucket("user-files")
                                    .object(from)
                                    .build())
                            .build()
            );

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket("user-files")
                    .object(from)
                    .build()
            );
            StatObjectResponse info = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("user-files")
                            .object(to)
                            .build()
            );
            if (from.endsWith("/")) {
                DirectoryResponseDto directoryResponseDto = new DirectoryResponseDto(
                        getParentFolder(to),
                        getNameFromPath(to),
                        DIRECTORY);
                return ResponseEntity.status(HttpStatus.OK).body(directoryResponseDto);
            }
            ResourceResponseDto resource = new ResourceResponseDto(
                    getParentFolder(to),
                    getNameFromPath(to),
                    info.size(),
                    FILE);
            return ResponseEntity.status(HttpStatus.OK).body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<?> createEmptyDirectory(String clientPath) {
        Long userId = getCurrentUserId();
        String path = buildFullPath(clientPath, userId);

        validateName(getNameFromPath(clientPath));
        try {
            InputStream emptyFolder = new ByteArrayInputStream(new byte[]{});
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("user-files")
                            .object(path)
                            .stream(emptyFolder, 0, 0)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        String displayName = getNameFromPath(path);
        String rootFolder = getParentFolder(path);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DirectoryResponseDto(
                        rootFolder,
                        displayName,
                        DIRECTORY
                ));
    }

    public ResponseEntity<?> searchResource (String query) {
        Long userId = getCurrentUserId();
        String path = buildFullPath("", userId);
        validateName(query);
        List<ResourceResponseDto> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket("user-files")
                            .prefix(path)
                            .build()
            );
            for (Result<Item> result : results) {

            if (result.get().objectName().contains(query)) {
                String parentFolder = getParentFolder(result.get().objectName());
                if (parentFolder.equals(getNameFromPath(result.get().objectName()))) {
                    parentFolder = getNameFromPath(result.get().objectName()).replaceAll("[^/]", "");
                }
                files.add(new ResourceResponseDto(
                        parentFolder,
                        getNameFromPath(result.get().objectName()),
                        result.get().size(),
                        result.get().objectName().endsWith("/") ? DIRECTORY : FILE
                ));
            }
            }
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok(files);
    }








    public String getNameFromPath(String path) {
        String[] elements = path.split("/");
        String lastElement = elements[elements.length - 1];
        return path.endsWith("/") ? lastElement + "/" : lastElement;
    }

    public String getParentFolder(String path) {
        String[] elements = path.split("/");
        String parent = Arrays.stream(elements)
                .skip(1)
                .limit(elements.length - 1)
                .collect(Collectors.joining("/"));
        return parent.endsWith("/") ? parent : parent + "/";
    }

    private String prefix(Long userId) {
        return USER_PREFIX.formatted(userId);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((MyUserDetails) auth.getPrincipal()).getId();
    }

    private String buildFullPath(String clientPath, Long userId) {
        if (clientPath.startsWith("/")) {
            clientPath = clientPath.substring(1);
        }
        return prefix(userId) + clientPath;
    }

    public void validateName(String name) {

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
}


