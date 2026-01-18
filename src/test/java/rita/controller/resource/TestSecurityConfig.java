package rita.controller.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import rita.security.MyUserDetails;
import rita.service.CustomUserDetailsService;

import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.*;

@Configuration
public class TestSecurityConfig {

    private static final Long TEST_USER_ID = 42L;
    private static final String TEST_USERNAME = "testuser";

    @Bean
    @Primary
    public CustomUserDetailsService testUserDetailsService() {

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