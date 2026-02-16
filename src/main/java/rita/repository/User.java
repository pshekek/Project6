package rita.repository;


import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@Builder
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
