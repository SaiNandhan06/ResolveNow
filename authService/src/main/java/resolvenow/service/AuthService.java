package resolvenow.service;

import resolvenow.dto.AuthResponse;
import resolvenow.dto.LoginRequest;
import resolvenow.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}