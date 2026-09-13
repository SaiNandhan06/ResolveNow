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
}