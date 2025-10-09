package rita.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Map;

//@RestController
//@RequestMapping("api/user")
//@RequiredArgsConstructor
//public class UserController {
//
//    private final UserService userService;
//    private final UserRepository userRepository;
//
//    @GetMapping("/me")
//    public ResponseEntity<UserDto> getCurrentUser() {
//        Authentication authentication = SecurityContextHolder
//                .getContext()
//                .getAuthentication();
//        String username = authentication.getName();
//        UserDto userDto = new UserDto();
//        userDto.setUsername(username);
//        return ResponseEntity.ok(userDto);
//    }
//}

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("username", authentication.getName()));
    }
}
