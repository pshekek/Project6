package rita.controller.resource;


import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rita.dto.MessageDto;
import rita.dto.ResourceResponseDto;
import rita.exeptions.EntityAlreadyExistsException;
import rita.exeptions.MinioException;
import rita.service.MinioService;
import rita.service.NamingService;

import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
@Tag(name = "Resources", description = "Методы для работы со всеми ресурсами")
public class ResourceController {

    private final MinioService minioService;
    private final NamingService namingService;

    @Operation(
            summary = "Скачивает файл или папку"
    )
    @GetMapping(path = "/download")
    public ResponseEntity<?> downloadResource(@RequestParam("path")
                                              @Parameter(description = "Путь к файлу") String path) {

        if (path == null || path.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageDto("Невалидный или отсутствующий путь"));
        }
        InputStreamResource resource;
        try {
            resource = minioService.downloadResource(path);
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new MessageDto("Ресурс не найден"));
        } catch (MinioException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDto("Неизвестная ошибка при скачивании файла"));
        }
        String filename = namingService.getNameFromPath(path);
        if (!filename.endsWith(".zip")) {
            filename += ".zip";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Operation(
            summary = "Загружает файл или папку"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResource(@RequestParam("object")
                                            @Parameter(description = "Список файлов в формате MultipartFile")
                                            List<MultipartFile> file,
                                            @Parameter(description = "Путь к файлу")
                                            @RequestParam("path") String path) {

        List<ResourceResponseDto> files;

        try {
            files = minioService.uploadFile(file, path);
        } catch (EntityAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageDto("Файл с таким именем уже существует"));
        } catch (MinioException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDto("Неизвестная ошибка при загрузке файла"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(files);
    }

    @Operation(
            summary = "Получает информацию о ресурсе"
    )
    @GetMapping
    public ResponseEntity<?> getInfo(@RequestParam("path")
                                     @Parameter(description = "Путь к файлу") String path) {
        if (path == null || path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageDto("Невалидный или отсутствующий путь"));
        }
        ResourceResponseDto dto;
        try {
            dto = minioService.getInfo(path);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageDto("Ресурс не найден"));
        } catch (MinioException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDto("Неизвестная ошибка: " + e.getMessage()));
        }
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Удаляет файл или папку"
    )
    @DeleteMapping
    public ResponseEntity<?> deleteResource(@RequestParam("path")
                                            @Parameter(description = "Путь к файлу") String path) {
        if (path == null || path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageDto("Невалидный или отсутствующий путь"));
        }
        try {
            minioService.deleteResource(path);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageDto("Ресурс не найден"));
        } catch (MinioException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDto("Ошибка при удалении ресурсов: " + e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(
            summary = "Перемещает или переименовывает файл или папку"
    )
    @GetMapping(path = "/move")
    public ResponseEntity<?> moveResource(@RequestParam("from")
                                          @Parameter(description = "Откуда перемещается файл") String from,
                                          @Parameter(description = "Куда перемещается файл")
                                          @RequestParam("to") String to) {
        ResourceResponseDto dto;

        if (from.equals(to)) {
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        try {
            dto = minioService.moveOrRenameResource(from, to);
        } catch (MinioException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDto("Ошибка при работе с ресурсами: " + e.getMessage()));

        } catch (EntityAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageDto("Файл с таким именем уже существует"));
        }
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @Operation(
            summary = "Поиск по всем файлам пользователя"
    )
    @GetMapping(path = "/search")
    public ResponseEntity<?> searchResource(@RequestParam("query")
                                            @Parameter(description = "Строка, которую вводит пользователь в поиск")
                                            String query) {
        List<ResourceResponseDto> files;

        try {
            files = minioService.searchResource(query);

        } catch (MinioException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDto("Ошибка при поиске ресурсов: " + e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.OK).body(files);
    }
}
