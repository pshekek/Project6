package rita.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter
@Getter
public class UserRegisterRequest {
    @NotBlank(message = "Введите юзернейм")
    private String username;

    @NotBlank(message = "Введите пароль")
    private String password;
}
