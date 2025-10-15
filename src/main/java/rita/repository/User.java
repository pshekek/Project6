package rita.repository;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "username",unique = true, nullable = false, length = 40)
    private String username;

    @Column(name = "password", nullable = false, length = 80)
    private String password;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserFolder folder;

    public User(String password, String username) {
        this.password = password;
        this.username = username;
    }
}
