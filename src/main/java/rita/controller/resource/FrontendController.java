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