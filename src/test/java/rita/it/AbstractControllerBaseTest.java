package rita.it;


import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractControllerBaseTest {


    static {

        // Указываем прямой путь к пайпу Docker Desktop
        System.setProperty("DOCKER_HOST", "npipe:////./pipe/docker_engine");
        // Отключаем Ryuk, так как он чаще всего виноват в зависании на Windows
        System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true");
    }


    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withUsername("root")
            .withPassword("root")
            .withDatabaseName("Project6_testcontainers");

    @Container
    static final GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server /data");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);


        registry.add("minio.url", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
    }
}