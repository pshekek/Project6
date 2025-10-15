package rita.controller.resource;


import io.minio.GetObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.dto.ResourceResponseDto;
import rita.exeptions.BaseResponse;
import rita.service.MinioService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/resource")
public class MinioController {

    private final MinioService minioService;


    @GetMapping
    public ResourceResponseDto getInfo(@RequestParam ("path") String path) {
       return minioService.getInfo(path);
    }

    @DeleteMapping
    public HttpStatus deleteResource(@RequestParam ("path") String path) {
        minioService.deleteResource(path);
        return HttpStatus.NO_CONTENT;
    }

    @GetMapping(path = "/download")
    public ResponseEntity<InputStreamResource> downloadResource(@RequestParam ("path") String path) {
        return ResponseEntity.ok(minioService.downloadResource(path));
    }


}
