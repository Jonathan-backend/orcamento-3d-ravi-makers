package br.com.orcamento3d.auth;

import br.com.orcamento3d.user.Role;
import br.com.orcamento3d.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final JwtService jwt = new JwtService(
            "segredo-de-teste-com-mais-de-quarenta-e-oito-caracteres-2026", 3600);

    @Test
    void rejectsTokenWhenAccountIsDisabled() {
        User account = new User();
        account.setEmail("admin@example.com");
        account.setName("Admin");
        account.setRole(Role.ADMIN);
        String token = jwt.generate(account);
        UserDetails disabled = org.springframework.security.core.userdetails.User
                .withUsername(account.getEmail())
                .password("hash")
                .roles("ADMIN")
                .disabled(true)
                .build();

        assertThat(jwt.isValid(token, disabled)).isFalse();
    }
}
