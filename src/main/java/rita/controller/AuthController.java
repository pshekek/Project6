package rita.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.dto.UserLoginRequest;
import rita.exeptions.BaseResponse;
import rita.service.UserService;


//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final UserService userService;
//
//
//    @PostMapping("/sign-in")
//    public ResponseEntity<BaseResponse<String>> login(@RequestBody UserLoginRequest request) {
//        BaseResponse<String> response =
//                userService.login(request.getUsername(), request.getPassword());
//        if (response.getErrorCode() != 200) {
//            return ResponseEntity.status(response.getErrorCode()).body(response);
//        }
//        return ResponseEntity.ok(response);
//    }
//
//
//}
