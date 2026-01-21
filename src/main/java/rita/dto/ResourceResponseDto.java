package rita.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import rita.repository.Type;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ResourceResponseDto {

    private String path;
    private String name;
    private Long size;
    private Type type;
}
