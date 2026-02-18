package rita.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.exeptions.EntityAlreadyExistsException;
import rita.mapping.UserMapping;
import rita.repository.User;
import rita.repository.UserFolder;
import rita.repository.UserRepository;


import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.ValidationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapping userMapping;
    private final AuthenticationManager authManager;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public UserDto create(@Valid UserRegisterRequest request, HttpServletRequest httpRequest) {

        userRepository.findByUsername(request.getUsername()).ifPresent(user -> {
            throw new EntityAlreadyExistsException("Пользователь с именем " + user.getUsername()
                    + " уже существует");
        });
        if (request.getUsername().length() <= 5) {
            throw new ValidationException("Юзернейм должен быть не меньше 5 символов");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User userToCreate = new User();
        userToCreate.setPassword(encodedPassword);
        userToCreate.setUsername(request.getUsername());

        UserFolder folder = new UserFolder();
        folder.setUser(userToCreate);
        userToCreate.setFolder(folder);

        userRepository.save(userToCreate);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authentication = authManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        httpRequest.getSession(true);

        return userMapping.toDto(userToCreate);
    }
}
