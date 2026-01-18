package rita.controller.resource;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rita.dto.ResourceResponseDto;
import rita.service.MinioService;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
@Tag(name = "Resources", description = "Методы для работы со всеми ресурсами")
public class ResourceController {

    private final MinioService minioService;

    @Operation(
            summary = "Скачивает файл или папку"
    )
    @GetMapping(path = "/download")
    public ResponseEntity<?> downloadResource(@RequestParam("path")
                                              @Parameter(description = "Путь к файлу") String path) {
        return minioService.downloadResource(path);
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
        return minioService.uploadFile(file, path);
    }

    @Operation(
            summary = "Получает информацию о ресурсе"
    )
    @GetMapping
    public ResponseEntity<?> getInfo(@RequestParam("path")
                                     @Parameter(description = "Путь к файлу") String path) {
        return minioService.getInfo(path);
    }

    @Operation(
            summary = "Удаляет файл или папку"
    )
    @DeleteMapping
    public ResponseEntity<?> deleteResource(@RequestParam("path")
                                     @Parameter(description = "Путь к файлу") String path) {
       return minioService.deleteResource(path);
    }

    @Operation(
            summary = "Перемещает или переименовывает файл или папку"
    )
    @GetMapping(path = "/move")
    public ResponseEntity<?> moveResource(@RequestParam("from")
                                          @Parameter(description = "Откуда перемещается файл") String from,
                                          @Parameter(description = "Куда перемещается файл")
                                          @RequestParam("to") String to) {
        return minioService.moveOrRenameResource(from, to);
    }

    @Operation(
            summary = "Поиск по всем файлам пользователя"
    )
    @GetMapping(path = "/search")
    public ResponseEntity<?> searchResource(@RequestParam("query")
                                            @Parameter(description = "Строка, которую вводит пользователь в поиск")
                                            String query) {
        return minioService.searchResource(query);
    }
}
