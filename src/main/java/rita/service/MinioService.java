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
import rita.dto.ResourceResponseDto;
import rita.repository.Resource;
import rita.repository.UserRepository;

import javax.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public ResponseEntity<?> getInfo(String path) {
        try {
            StatObjectResponse statObject = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket("user-files")
                            .object(path)
                            .build()
            );
        } catch (
                EntityNotFoundException e) {
            throw new EntityNotFoundException("Такого ресурса нет");
        }

        try {
            if (path.endsWith("/")) {
                return ResponseEntity.ok()
                        .body(new DirectoryResponseDto(
                                path,
                                getNameFromPath(path),
                                DIRECTORY
                        ));
            } else {
                StatObjectResponse statObject = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket("user-files")
                                .object(path)
                                .build()
                );
                long size = statObject.size();
                return ResponseEntity.ok()
                        .body(new ResourceResponseDto(
                                path,
                                getNameFromPath(path),
                                size,
                                FILE
                        ));
            }
        } catch (
                Exception e) {
            throw new RuntimeException("Ошибка получения информации из minioClient", e);
        }

    }

    public void deleteResource(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket("user-files")
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка удаления из minio", e);
        }
    }

    public ResponseEntity<?> downloadResource(String path) {

        if (path == null || path.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Невалидный или отсутствующий путь");
        }
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
                    hasFiles = true;
                    Item item = result.get();
                    try (InputStream inputStream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket("user-files")
                                    .object(item.objectName())
                                    .build()
                    )) {
                        ZipEntry zipEntry = new ZipEntry(item.objectName());
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


    public ResponseEntity<?> showAllFilesFromFolder(String path, String username) {
        if (path.isEmpty()) {
            Long userId = userRepository.findByUsername(username).get().getId();
            path = +userId + "/" + path;
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
                String fileName = getNameFromPath(objectName);
                files.add(new ResourceResponseDto(path, fileName, item.size(),
                        item.objectName().endsWith("/") ? DIRECTORY : FILE));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok()
                .body(files);
    }


    public ResponseEntity<?> uploadFile(MultipartFile multipartFile, String path, String username) {
        Long userId = userRepository.findByUsername(username).get().getId();
        if (path.isEmpty()) {
            String pathRoot = multipartFile.getOriginalFilename();
            try (InputStream inputStream = multipartFile.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket("user-files")
                                .object(userId + "/" + pathRoot)
                                .stream(inputStream, multipartFile.getSize(), -1)
                                .contentType(multipartFile.getContentType())
                                .build());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            String pathRoot = userId + "/" + path + (multipartFile.getOriginalFilename());
            if (!path.endsWith("/")) {
                pathRoot = userId + "/" + path + "/" + (multipartFile.getOriginalFilename());
            }
            try (InputStream inputStream = multipartFile.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket("user-files")
                                .object(pathRoot)
                                .stream(inputStream, multipartFile.getSize(), -1)
                                .contentType(multipartFile.getContentType())
                                .build());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        String originalFileName = multipartFile.getOriginalFilename();
        String displayName = getNameFromPath(originalFileName);
        String parent = userId + "/" + path + "/";
        String parent2 = parent.substring(0, parent.lastIndexOf('/'));
        List<ResourceResponseDto> files = List.of(new ResourceResponseDto(
                parent2,
                displayName,
                multipartFile.getSize(),
                multipartFile.getName().endsWith("/") ? DIRECTORY : FILE));

        return ResponseEntity.status(HttpStatus.CREATED).body(files);
    }

    public ResponseEntity<?> moveOrRenameResource(String from, String to, String username) {
        to = userRepository.findByUsername(username).get().getId() + "/" + to;
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
                        to,
                        getNameFromPath(to),
                        DIRECTORY);
                return ResponseEntity.status(HttpStatus.OK).body(directoryResponseDto);
            }
            ResourceResponseDto resource = new ResourceResponseDto(
                    to,
                    getNameFromPath(to),
                    info.size(),
                    FILE);
            return ResponseEntity.status(HttpStatus.OK).body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<?> createEmptyDirectory(String path, String username) {
        Long userId = userRepository.findByUsername(username).get().getId();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        String fullPath = userId + "/" + path;

        try {
            InputStream emptyFolder = new ByteArrayInputStream(new byte[]{});
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket("user-files")
                            .object(fullPath)
                            .stream(emptyFolder, 0, 0)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        String parent = fullPath.substring(0, fullPath.lastIndexOf('/', fullPath.length() - 2) + 1);
        String name = getNameFromPath(fullPath) + "/";
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new DirectoryResponseDto(
                        parent,
                        name,
                        DIRECTORY
                ));
    }

    public String getNameFromPath(String path) {
        String[] elements = path.split("/");
        String lastElement = elements[elements.length - 1];
        return path.endsWith("/") ? lastElement + "/" : lastElement;
    }
}


