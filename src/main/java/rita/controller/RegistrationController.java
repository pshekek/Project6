package rita.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.repository.User;
import rita.security.jwt.JwtService;
import rita.service.UserService;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/sign-up")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;
    private final JwtService jwtService;


    @PostMapping
    public ResponseEntity<Map<String, String>> createUser(@RequestBody @Valid UserRegisterRequest request) {

        UserDto userDto = userService.create(request);

        String token = jwtService.generateJwtToken(userDto.getUserName());
        String refreshToken = jwtService.generateRefreshToken(userDto.getUserName());

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);

        return ResponseEntity.ok(response);
    }
}
