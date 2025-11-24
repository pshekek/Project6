package rita.controller.resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
public class FrontendController {
    @GetMapping(value = {"/registration", "/login", "/files/**"})
    public String handleRefresh() {
        return "forward:/index.html";
    }




}

//   private final MinioService minioService;
//
//    @GetMapping
//    public ResponseEntity<?> getRootFiles(Principal principal) {
//        String username = principal.getName();
//        return minioService.showAllFilesFromFolder("", username);
//    }
//
//    @GetMapping("/{path}")
//    public ResponseEntity<?> getFiles(@PathVariable String path, Principal principal) {
//        String username = principal.getName();
//        return minioService.showAllFilesFromFolder(path, username);
//    }
//    }

//
//@GetMapping
//public ResponseEntity<?> showAllFilesFromFolder(@RequestParam("path") String path, Principal principal) {
//    String username = principal.getName();
//    return minioService.showAllFilesFromFolder(path, username);
//}