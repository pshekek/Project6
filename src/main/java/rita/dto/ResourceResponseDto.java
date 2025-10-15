package rita.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rita.repository.Type;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponseDto {

    private String name;
    private String path;
    private Long size;
    private Type type;
}
