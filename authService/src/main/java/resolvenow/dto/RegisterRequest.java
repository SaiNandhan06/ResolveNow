package resolvenow.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;

    @Email @NotBlank
    private String email;

    @NotBlank @Size(min = 8)
    private String password;

    // Optional: "AGENT" callers supply these; absent => defaults to CUSTOMER with no department
    private String role;
    private String department;
}