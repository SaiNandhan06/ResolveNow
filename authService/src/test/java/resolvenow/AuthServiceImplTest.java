package resolvenow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import resolvenow.dto.RegisterRequest;
import resolvenow.model.User;
import resolvenow.repo.RefreshTokenRepository;
import resolvenow.repo.UserRepository;
import resolvenow.security.JwtUtil;
import resolvenow.service.impl.AuthServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthServiceImpl service;

    @Test
    void publicRegistrationAlwaysCreatesCustomer() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("customer");
        request.setEmail("customer@example.com");
        request.setPassword("password");
        request.setRole("AGENT");
        when(userRepository.existsByUsername("customer")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(1L);
            return user;
        });
        when(jwtUtil.generateAccessToken(1L, "CUSTOMER", null)).thenReturn("token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh");

        var response = service.register(request);

        assertEquals("CUSTOMER", response.getRole());
        verify(userRepository).save(argThat(user -> "CUSTOMER".equals(user.getRole())));
    }
}
