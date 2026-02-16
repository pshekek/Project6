package rita.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rita.dto.UsernameResponse;


import java.security.Principal;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Operation(
            summary = "Получает текущего пользователя по id"
    )
    @GetMapping("/me")
    public ResponseEntity<UsernameResponse> getCurrentUser(@Parameter(description = "Данные пользователя из Spring")
                                                               Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
     UsernameResponse usernameResponse = new UsernameResponse(principal.getName());
        return ResponseEntity.status(HttpStatus.OK).body(usernameResponse);
    }
}
