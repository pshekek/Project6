package rita.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NamingService {

    public String getNameFromPath(String path) {
        String[] elements = path.split("/");
        String lastElement = elements[elements.length - 1];
        return path.endsWith("/") ? lastElement + "/" : lastElement;
    }
    public String getParentFolder(String path) {
        String[] elements = path.split("/");
        String parent = Arrays.stream(elements)
                .skip(1)
                .limit(elements.length - 1)
                .collect(Collectors.joining("/"));
        return parent.endsWith("/") ? parent : parent + "/";
    }
}
