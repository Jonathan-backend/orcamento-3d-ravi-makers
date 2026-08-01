package br.com.orcamento3d.auth;

import br.com.orcamento3d.auth.AuthDtos.*;
import br.com.orcamento3d.user.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final boolean secureCookie;

    public AuthController(UserRepository users, PasswordEncoder encoder,
                          AuthenticationManager authManager, JwtService jwt,
                          @Value("${app.auth.secure-cookie:false}") boolean secureCookie) {
        this.users = users; this.encoder = encoder; this.authManager = authManager; this.jwt = jwt;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("E-mail já cadastrado"));
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(encoder.encode(request.password()));
        user.setRole(Role.ADMIN);
        users.save(user);
        setCookie(response, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response(user));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(), request.password()));
        User user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        if(user.getRole()!=Role.ADMIN||user.getAccountOwner()!=null)
            throw new DisabledException("Esta conta não é uma conta administradora.");
        setCookie(response, user);
        return response(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("ORCA_AUTH", "")
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/").maxAge(0).build().toString());
        return ResponseEntity.noContent().build();
    }

    private void setCookie(HttpServletResponse response, User user) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("ORCA_AUTH", jwt.generate(user))
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/")
                .maxAge(jwt.getExpirationSeconds()).build().toString());
    }

    private AuthResponse response(User user) {
        return new AuthResponse(null, "Cookie", jwt.getExpirationSeconds(),
                user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public record ApiError(String message) {}
}
