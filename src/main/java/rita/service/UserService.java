package rita.service;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.exeptions.EntityAlreadyExistsException;
import rita.mapping.UserMapping;
import rita.repository.User;
import rita.repository.UserRepository;
import rita.security.jwt.JwtService;

import javax.validation.Valid;

@Service
@AllArgsConstructor
public class UserService {

    UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapping userMapping;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public UserDto create(@Valid UserRegisterRequest request) {

        userRepository.findByUsername(request.getUserName()).ifPresent(user -> {
            throw new EntityAlreadyExistsException("Пользователь с именем " + user.getUsername()
                    + " уже существует");
        });
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User userToCreate = userMapping.toEntityCreate(request);
        userToCreate.setPassword(encodedPassword);
        userRepository.save(userToCreate);
        return userMapping.toDto(userToCreate);
    }
}
