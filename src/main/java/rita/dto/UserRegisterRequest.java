package rita.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter
@Getter
public class UserRegisterRequest {
    @NotBlank(message = "Введите юзернейм")
    private String userName;

    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;
}
