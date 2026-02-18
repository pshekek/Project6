package rita.controller.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import rita.dto.MessageDto;
import rita.dto.ResourceResponseDto;
import rita.exeptions.EntityAlreadyExistsException;
import rita.exeptions.MinioException;
import rita.service.MinioService;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static rita.repository.Type.DIRECTORY;


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

        List<ResourceResponseDto> files;

        try {
            files = minioService.showAllFilesFromFolder(path);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageDto("Папки не существует"));
        } catch (MinioException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageDto("Неизвестная ошибка при скачивании файла"));
    }
        return ResponseEntity.ok(files);
    }

    @Operation(
            summary = "Создаёт пустую папку"
    )
    @PostMapping
    public ResponseEntity<?> createEmptyDirectory(@RequestParam("path")
                                                  @Parameter(description = "Путь к папке") String path) {
        ResourceResponseDto folder;
        try {
            folder = minioService.createEmptyDirectory(path);

        } catch (EntityAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new MessageDto("Папка с таким именем уже существует"));
        } catch (MinioException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(folder);
    }
}
