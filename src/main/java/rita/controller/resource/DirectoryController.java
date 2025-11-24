package rita.controller.resource;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import rita.service.MinioService;

import java.security.Principal;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/directory")
public class DirectoryController {

    private final MinioService minioService;

    @GetMapping
    public ResponseEntity<?> showAllFilesFromFolder(@RequestParam("path") String path, Principal principal) {
        String username = principal.getName();
        return minioService.showAllFilesFromFolder(path, username);
    }

    @PostMapping
    public ResponseEntity<?> createEmptyDirectory(@RequestParam("path") String path, Principal principal) {
        String username = principal.getName();
        return minioService.createEmptyDirectory(path, username);
    }
}
