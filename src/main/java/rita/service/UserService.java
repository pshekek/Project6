package rita.service;

import lombok.AllArgsConstructor;
import org.apache.tomcat.websocket.AuthenticationException;
import org.mapstruct.control.MappingControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;
import rita.dto.JwtAuthenticationDTO;
import rita.dto.UserCredentialsDTO;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.exeptions.BaseResponse;
import rita.exeptions.EntityAlreadyExistsException;
import rita.mapping.UserMapping;
import rita.repository.User;
import rita.repository.UserRepository;
import rita.security.jwt.JwtService;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {

    UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapping userMapping;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public UserDto create(@Valid UserRegisterRequest request) {

        userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            throw new EntityAlreadyExistsException("Пользователь с именем " + user.getUsername()
                    + " уже существует");
        });
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User userToCreate = new User();
        userToCreate.setPassword(encodedPassword);
        userToCreate.setUsername(request.getUsername());
        userRepository.save(userToCreate);
        return userMapping.toDto(userToCreate);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public BaseResponse<String> login(@RequestParam String username,
                                      @RequestParam String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (user == null) {
            return BaseResponse.error(401, "Неверный юзернейм");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return BaseResponse.error(401, "Неверный пароль");
        }
        JwtAuthenticationDTO authDto = jwtService.generateAuthToken(username);
        String token = authDto.getToken();
        return BaseResponse.success(token);
    }

    public JwtAuthenticationDTO signIn(UserCredentialsDTO userCredentialsDTO) throws AuthenticationException {
        User user = findByCredentials(userCredentialsDTO);
        return jwtService.generateAuthToken(user.getUsername());
    }

    private User findByCredentials(UserCredentialsDTO userCredentialsDTO) throws AuthenticationException {
        Optional<User> optionalUser = userRepository.findByUsername(userCredentialsDTO.getUsername());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(userCredentialsDTO.getPassword(), user.getPassword())) {
                return user;
            }
        }
        throw new AuthenticationException("Неверный логин или пароль");
    }
}
