package rita.controller.resource;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import rita.dto.ResourceResponseDto;
import rita.service.MinioService;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resource")
public class ResourceController {

    private final MinioService minioService;

    @GetMapping(path = "/download")
    public ResponseEntity<?> downloadResource(@RequestParam("path") String path) {
        return minioService.downloadResource(path);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResource(@RequestParam("object") MultipartFile file,
                                            @RequestParam("path") String path) {
        return minioService.uploadFile(file,path);
    }


    @GetMapping
    public ResponseEntity<?> getInfo(@RequestParam("path") String path) {
        return minioService.getInfo(path);
    }

    @DeleteMapping
    public HttpStatus deleteResource(@RequestParam("path") String path) {
        minioService.deleteResource(path);
        return HttpStatus.NO_CONTENT;
    }

    @GetMapping(path = "/move")
    public ResponseEntity<?> moveResource(@RequestParam("from") String from,
                                          @RequestParam("to") String to) {
        return minioService.moveOrRenameResource(from, to);
    }

    @GetMapping(path = "/search")
    public ResponseEntity<?> searchResource(@RequestParam("query") String query) {
        return minioService.searchResource(query);
    }
}
