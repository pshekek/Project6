package rita.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter
@Getter
@AllArgsConstructor
public class UserRegisterRequest {
    @NotBlank(message = "Введите юзернейм")
    @Schema(description = "Имя пользователя", example = "Anna")
    private String username;

    @NotBlank(message = "Введите пароль")
    @Schema(description = "Пароль", example = "12345")
    private String password;
}
