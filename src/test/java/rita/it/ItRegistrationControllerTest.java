package rita.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import rita.dto.UserRegisterRequest;
import rita.repository.UserRepository;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class ItRegistrationControllerTest extends AbstractControllerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Test Create User functionality - Success")
    public void givenValidUserRequest_whenSignUp_thenCreatedResponse() throws Exception {
        // Given
        UserRegisterRequest request = new UserRegisterRequest("new_user", "strongPassword123");

        // When
        mockMvc.perform(post("/api/auth/sign-up")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username", CoreMatchers.is("new_user")));
    }

    @Test
    @DisplayName("Test Create User with existing username - Failure")
    public void givenExistingUsername_whenSignUp_thenConflictResponse() throws Exception {
        // Given
        UserRegisterRequest request = new UserRegisterRequest("duplicate_user", "password");

        mockMvc.perform(post("/api/auth/sign-up")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When
        mockMvc.perform(post("/api/auth/sign-up")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict());
    }
}



//@SpringBootTest
//@TestExecutionListeners(listeners = {
//        ServletTestExecutionListener.class,
//        WithSecurityContextTestExecutionListener.class},
//        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
//@Transactional
//class ItRegistrationControllerTest {
//
//    @Autowired
//    private WebApplicationContext wac;
//
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    private static final String AUTH_ME_URL = "/api/user/me";
//
//    private static final String SIGN_UP_URL = "/api/auth/sign-up";
//
//
//
//    @BeforeEach
//    void setUp() {
//        this.mockMvc = webAppContextSetup(this.wac)
//                .apply(springSecurity())
//                .alwaysDo(MockMvcResultHandlers.print())
//                .build();
//    }
//
//
//    @Test
//    void createUserSuccess() throws Exception {
//        MockHttpSession session = new MockHttpSession();
//        final String expectedUsername = "admin2";
//        final String password = "admin2";
//
//        UserRegisterRequest user = new UserRegisterRequest(
//                expectedUsername,
//                password
//        );
//
//        String userJson = objectMapper.writeValueAsString(user);
//
//        long initialUserCount = userRepository.count();
//
//        MvcResult result = mockMvc.perform(
//                        post(SIGN_UP_URL)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(userJson)
//                                .session(session)
//                )
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.username").value(expectedUsername))
//                .andReturn();
//
//
//        assertThat(userRepository.count()).isEqualTo(initialUserCount + 1);
//
//        assertThat(userRepository.findByUsername(expectedUsername)).isPresent();
//
//        MockHttpSession authenticatedSession = (MockHttpSession) result.getRequest().getSession(false);
//
//        assertThat(authenticatedSession).isNotNull();
//
//        mockMvc.perform(
//                        get(AUTH_ME_URL)
//                                .session(authenticatedSession)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.username").value(expectedUsername));
//
//    }
//
//    @Test
//    @Sql(scripts = "/data/insertData.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
//    void createUserConflict() throws Exception {
//
//        final String expectedUsername = "admin";
//        final String password = "admin";
//
//        UserRegisterRequest user = new UserRegisterRequest(
//                expectedUsername,
//                password
//        );
//        String userJson = objectMapper.writeValueAsString(user);
//        long initialUserCount = userRepository.count();
//        assertThat(initialUserCount).isGreaterThan(0);
//
//        MvcResult result = mockMvc.perform(
//                        post(SIGN_UP_URL)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .content(userJson)
//                )
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.description").value(containsString("уже существует")))
//                .andReturn();
//        assertThat(userRepository.count()).isEqualTo(initialUserCount);
//    }
//}