package rita.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rita.dto.AuthResponse;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.security.jwt.JwtService;
import rita.service.UserService;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;
    private final JwtService jwtService;


    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> createUser(@RequestBody @Valid UserRegisterRequest request) {

        UserDto userDto = userService.create(request);

        String token = jwtService.generateJwtToken(userDto.getUsername());
        String refreshToken = jwtService.generateRefreshToken(userDto.getUsername());
        AuthResponse response = new AuthResponse(userDto.getUsername(), token, refreshToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
