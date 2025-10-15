package rita.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import rita.dto.ResourceResponseDto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static rita.repository.Type.DIRECTORY;
import static rita.repository.Type.FILE;

@Service
@RequiredArgsConstructor
@Log4j2
public class MinioService {

    private final MinioClient minioClient;

    public ResourceResponseDto getInfo(String path) {

        try {
            if (path.endsWith("/")) {
                return new ResourceResponseDto(
                        getNameFromPath(path),
                        path,
                        null,
                        DIRECTORY
                );
            } else {
                StatObjectResponse statObject = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket("user-files")
                                .object(path)
                                .build()
                );
                long size = statObject.size();
                return new ResourceResponseDto(
                        getNameFromPath(path),
                        path,
                        size,
                        FILE
                );
            }
        } catch (Exception e) {
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

    public InputStreamResource downloadResource(String path) {
        if (!path.endsWith("/")) {
            try {
                InputStreamResource resource = new InputStreamResource(minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket("user-files")
                                .object(path)
                                .build()
                ));
                return resource;
            } catch (Exception e) {
                throw new RuntimeException("Ошибка загрузки из minio", e);
            }
        } else {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream);
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket("user-files")
                                .prefix(path)
                                .build()
                );
                for (Result<Item> result : results) {
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
                InputStreamResource resource = new InputStreamResource
                        (new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                return resource;

            } catch (Exception e) {
                throw new RuntimeException("Ошибка загрузки из minio", e);
            }
        }
    }


    public String getNameFromPath(String path) {
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int index = path.lastIndexOf("/");
        path = path.substring(index + 1);
        return path;
    }
}

