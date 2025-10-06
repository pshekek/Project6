package rita.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class UserLoginRequest {
    @NotBlank(message = "Введите юзернейм")
    private String userName;
    @NotBlank(message = "Введите пароль")
    private String password;
}
