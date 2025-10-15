package rita.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.AllArgsConstructor;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rita.dto.UserCredentialsDTO;
import rita.dto.UserDto;
import rita.dto.UserRegisterRequest;
import rita.exeptions.EntityAlreadyExistsException;
import rita.mapping.UserMapping;
import rita.repository.User;
import rita.repository.UserFolder;
import rita.repository.UserRepository;


import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static rita.repository.Type.DIRECTORY;

@Service
@AllArgsConstructor
public class UserService {

    UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapping userMapping;
    private final MinioService minioService;
    private final MinioClient minioClient;

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

        UserFolder folder = new UserFolder();
        folder.setUser(userToCreate);
        userToCreate.setFolder(folder);

        userRepository.save(userToCreate);

        String folderPath = "user" + userToCreate.getId() + "/";
        try {
            InputStream emptyFolder = new ByteArrayInputStream(new byte[]{});

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket("user-files")
                    .object(folderPath)
                    .stream(emptyFolder, 0, -1)
                    .contentType("application/octet-stream")
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании папки пользователя", e);
        }
        return userMapping.toDto(userToCreate);
    }



}
