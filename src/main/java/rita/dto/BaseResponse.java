package rita.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse <T> {
    @Schema(description = "Код ответа или ошибки", example = "200")
    private int errorCode;
    @Schema(description = "Описание ответа", example = "OK или CONFLICT")
    private String description;
    @Schema(description = "Тело ответа. В случае ошибки отсутствует")
    private T body;

    public static <T> BaseResponse<T> success (T body) {
        return new BaseResponse<>(
                200,
                "OK",
                body
        );
    }
    public static <T> BaseResponse<T> error (int errorCode, String description) {
        return new BaseResponse<>(
                errorCode,
                description,
                null
        );
    }

}

