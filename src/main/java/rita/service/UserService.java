package rita.service;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rita.dto.ResourceResponseDto;
import rita.dto.UserCredentialsDTO;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static rita.repository.Type.DIRECTORY;
import static rita.repository.Type.FILE;

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
        if (request.getUsername().length() < 5) {
            throw new ValidationException("Юзернейм должен быть больше 5 символов");
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
