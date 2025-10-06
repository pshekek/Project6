package rita.exeptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse <T> {
    private int errorCode;
    private String description;
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

