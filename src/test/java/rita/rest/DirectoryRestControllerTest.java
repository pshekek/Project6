package rita.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import rita.controller.resource.DirectoryController;
import rita.dto.DirectoryResponseDto;
import rita.dto.MessageDto;
import rita.repository.Type;
import rita.security.AuthenticationHelperImpl;
import rita.service.MinioService;
import rita.service.UserService;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(controllers = DirectoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DirectoryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinioService minioService;



    @Test
    @SneakyThrows
    @DisplayName("Test Create empty Folder functionality")
    public void givenClientPath_whenCreateEmptyDirectory_thenSuccessResponse() {

        //given
        DirectoryResponseDto dto = new DirectoryResponseDto("path/folder/", "folder/", Type.DIRECTORY);
        ResponseEntity<?> body = ResponseEntity.status(HttpStatus.CREATED).body(dto);

        //when

        doReturn(body).when(minioService).createEmptyDirectory(anyString());

        //then

        mockMvc.perform(post("/api/directory")
                        .with(csrf())
                        .param("path", "path/folder/"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("path/folder/")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.is("folder/")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.is("DIRECTORY")));
    }

    @Test
    @SneakyThrows
    @DisplayName("Test Create empty Folder when path already exist functionality")
    public void givenExistingClientPath_whenCreateEmptyDirectory_thenConflictResponse() {

        //given

        ResponseEntity<?> response = ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageDto("Файл с таким именем уже существует"));

        //when

        doReturn(response).when(minioService).createEmptyDirectory(anyString());

        //then

        mockMvc.perform(post("/api/directory")
                        .with(csrf())
                        .param("path", "path/folder/"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message", CoreMatchers.is("Файл с таким именем уже существует")));
    }

}
