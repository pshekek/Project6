package rita.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import rita.security.MyUserDetails;

import java.util.Collection;
import java.util.List;
import rita.service.CustomUserDetailsService;

import static org.mockito.Mockito.*;

@TestConfiguration
@EnableWebSecurity
@Order(0)
public class TestSecurityConfig {

    private static final Long TEST_USER_ID = 42L;
    private static final String TEST_USERNAME = "testuser";

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
            .antMatchers("/api/auth/sign-up").permitAll()
            .antMatchers("/api/auth/sign-in").permitAll()
            .anyRequest().authenticated()
            .and()
            .httpBasic().disable()
            .formLogin().disable();

        return http.build();
    }

    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {

        CustomUserDetailsService mockService = mock(CustomUserDetailsService.class);
        MyUserDetails mockUserDetails = mock(MyUserDetails.class);

        when(mockUserDetails.getId()).thenReturn(TEST_USER_ID);
        when(mockUserDetails.getUsername()).thenReturn(TEST_USERNAME);
        when(mockUserDetails.getPassword()).thenReturn("protected_password");

        Collection<? extends GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        );
        doReturn(authorities)
            .when(mockUserDetails)
            .getAuthorities();

        when(mockService.loadUserByUsername(TEST_USERNAME)).thenReturn(mockUserDetails);

        return mockService;
    }
}