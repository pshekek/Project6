package rita.controller.user;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rita.dto.UserRegisterRequest;
import rita.service.UserService;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;


    @PostMapping("/sign-up")
    public ResponseEntity<?> createUser(@RequestBody @Valid UserRegisterRequest request) {
        userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "message", "Пользователь создан"));

    }
}