package rita.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.dto.UserLoginRequest;
import rita.exeptions.BaseResponse;
import rita.service.UserService;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;


    @PostMapping("/sign-in")
    public ResponseEntity<BaseResponse<String>> login(@RequestBody UserLoginRequest request) {
        BaseResponse<String> response =
                userService.login(request.getUsername(), request.getPassword());
        if (response.getErrorCode() != 200) {
            return ResponseEntity.status(response.getErrorCode()).body(response);
        }
        return ResponseEntity.ok(response);
    }

//    @PostMapping ("/sign-in")
//    public ResponseEntity <JwtAuthenticationDTO> signIn(@RequestBody UserCredentialsDTO credentials) {
//        try {
//            JwtAuthenticationDTO jwtAuthenticationDTO = userService.signIn(credentials);
//            return ResponseEntity.ok(jwtAuthenticationDTO);
//        } catch (AuthenticationException e) {
//            throw new RuntimeException("Authentication failed" + e.getMessage());
//        }
//    }
//    @PostMapping ("/refresh")
//    public JwtAuthenticationDTO refresh(@RequestBody RefreshTokenDto refreshTokenDto) throws Exception {
//        return userService.refreshToken(refreshTokenDto);
//    }


}
