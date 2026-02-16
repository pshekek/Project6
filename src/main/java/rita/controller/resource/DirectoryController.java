package rita.controller.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import rita.service.MinioService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
@Tag(name = "Directories", description = "Методы для работы с папками")
public class DirectoryController {

    private final MinioService minioService;

    @Operation(
            summary = "Показывает все файлы в папке"
    )
    @GetMapping
    public ResponseEntity<?> showAllFilesFromFolder(@RequestParam("path")
                                                    @Parameter(description = "Путь к папке") String path) {
        return minioService.showAllFilesFromFolder(path);
    }

    @Operation(
            summary = "Создаёт пустую папку"
    )
    @PostMapping
    public ResponseEntity<?> createEmptyDirectory(@RequestParam("path")
                                                  @Parameter(description = "Путь к папке") String path) {
        return minioService.createEmptyDirectory(path);
    }
}
