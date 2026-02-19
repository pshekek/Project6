package rita.rest;

import lombok.SneakyThrows;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import rita.controller.resource.DirectoryController;
import rita.dto.ResourceResponseDto;
import rita.exeptions.EntityAlreadyExistsException;
import rita.repository.Type;
import rita.service.MinioService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
        ResourceResponseDto dto = new ResourceResponseDto(
            "path/folder/",
            "folder/",
            null,
            Type.DIRECTORY
        );

        //when
        when(minioService.createEmptyDirectory(anyString())).thenReturn(dto);

        //then
        mockMvc.perform(post("/api/directory")
                .with(csrf())
                .param("path", "path/folder/")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.path", CoreMatchers.is("path/folder/")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.is("folder/")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", CoreMatchers.is("DIRECTORY")));

        verify(minioService, times(1)).createEmptyDirectory("path/folder/");
    }

    @Test
    @SneakyThrows
    @DisplayName("Test Create empty Folder when path already exist functionality")
    public void givenExistingClientPath_whenCreateEmptyDirectory_thenConflictResponse() {
        //given
        //when
        doThrow(new EntityAlreadyExistsException("Папка с таким именем уже существует"))
            .when(minioService).createEmptyDirectory(anyString());

        //then
        mockMvc.perform(post("/api/directory")
                .with(csrf())
                .param("path", "path/folder/")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isConflict())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message",
                CoreMatchers.is("Папка с таким именем уже существует")));

        verify(minioService, times(1)).createEmptyDirectory("path/folder/");
    }
}