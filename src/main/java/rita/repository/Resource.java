package rita.repository;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.persistence.*;


@Getter
@Setter
@NoArgsConstructor
public class Resource {
    private int id;
    private String name;
    private String path;
    private long size;
    private Type type;
}
