package rita.repository;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Test save user functionality")
    public void givenUserObject_whenSave_thenUserIsCreated() {
        UserFolder userFolder = new UserFolder();
        //given
        User userToSave = User.builder()
                .username("Username")
                .password("12345")
                .folder(userFolder)
                .build();
        //when
        User savedUser = userRepository.save(userToSave);

        //then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
    }
}
