package rita.controller.user;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.dto.UsernameResponse;
import rita.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;


    @PostMapping("/sign-up")
    public ResponseEntity<UsernameResponse> createUser(@RequestBody @Valid UserRegisterRequest request, HttpServletRequest httpRequest) {
        UserDto user = userService.create(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsernameResponse(user.getUsername()));

    }
}