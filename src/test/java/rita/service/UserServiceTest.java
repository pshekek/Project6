package rita.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.exeptions.EntityAlreadyExistsException;
import rita.mapping.UserMapping;
import rita.repository.User;
import rita.repository.UserRepository;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.ValidationException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapping userMapping;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UserService userService;


    @Test
    @DisplayName("Test user already exists")
    public void givenUser_whenUserAlreadyExists_thenThrowEntityAlreadyExistsException() {

        UserRegisterRequest request = new UserRegisterRequest("user123", "password");

        when(userRepository.findByUsername("user123"))
                .thenReturn(Optional.of(new User()));

        assertThrows(EntityAlreadyExistsException.class,
                () -> userService.create(request, httpRequest));
    }

    @Test
    @DisplayName("Test username is too short")
    void givenUser_whenUsernameTooShort_thenThrowValidationException() {

        UserRegisterRequest request = new UserRegisterRequest("user", "password");

        when(userRepository.findByUsername("user"))
                .thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () ->
                userService.create(request, httpRequest));

    }

    @Test
    @DisplayName("Test user is created")
    void givenUserObject_whenUserSave_thenUserIsCreated() {
        UserRegisterRequest request = new UserRegisterRequest("user123", "password");
        HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
        HttpSession httpSession = Mockito.mock(HttpSession.class);
        when(httpRequest.getSession(true)).thenReturn(httpSession);

        when(userRepository.findByUsername("user123"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password"))
                .thenReturn("encoded password");

        Authentication auth = Mockito.mock(Authentication.class);
        when(authManager.authenticate(any()))
                .thenReturn(auth);

        UserDto userDto = new UserDto();
        userDto.setUsername("user123");
        when(userMapping.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = userService.create(request, httpRequest);

        assertNotNull(result);
        assertEquals("user123", result.getUsername());

        Mockito.verify(userRepository).findByUsername("user123");
        Mockito.verify(passwordEncoder).encode("password");
        Mockito.verify(userRepository).save(any(User.class));
        Mockito.verify(authManager).authenticate(any());
        Mockito.verify(httpRequest).getSession(true);
        Mockito.verify(userMapping).toDto(any(User.class));

    }

    @Test
    @DisplayName("Test user not found by Username")
    public void givenUserIsNotFound_whenLoadUserByUsername_thenThrowUsernameNotFoundException() {
        //given
        CustomUserDetailsService user = new CustomUserDetailsService(userRepository);
        String username = "RRRrrrr";

        //when
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        //then
        assertThrows(UsernameNotFoundException.class, () ->
                user.loadUserByUsername(username));

    }

}
