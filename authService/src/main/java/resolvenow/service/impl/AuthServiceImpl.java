package resolvenow.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import resolvenow.dto.AuthResponse;
import resolvenow.dto.LoginRequest;
import resolvenow.dto.RegisterRequest;
import resolvenow.model.RefreshToken;
import resolvenow.model.User;
import resolvenow.repo.RefreshTokenRepository;
import resolvenow.repo.UserRepository;
import resolvenow.security.JwtUtil;
import resolvenow.service.AuthService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("CUSTOMER")
                .enabled(true)
                .build();

        user = userRepository.save(user);

        return issueTokens(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getRole());
        String refreshTokenValue = jwtUtil.generateRefreshToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshTokenValue)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .role(user.getRole())
                .userId(user.getUserId())
                .build();
    }
}