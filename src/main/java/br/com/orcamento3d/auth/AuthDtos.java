package br.com.orcamento3d.auth;

import br.com.orcamento3d.user.Role;
import jakarta.validation.constraints.*;

public final class AuthDtos {
    private AuthDtos() {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @Email @NotBlank @Size(max = 120) String email,
            @NotBlank
            @Size(min = 10, max = 72)
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                    message = "A senha deve conter pelo menos uma letra e um número")
            String password) {}
    public record AuthResponse(String token, String type, long expiresIn, Long userId, String name, String email, Role role) {}
}
