package br.com.orcamento3d.feedback;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupportAccessTest {
    private final SupportAccess access = new SupportAccess("jonathangtec@gmail.com, outro@example.com");

    @Test
    void permitsConfiguredAdministratorIgnoringCase() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "JONATHANGTEC@GMAIL.COM", "n/a", java.util.List.of());
        assertThat(access.allowed(auth)).isTrue();
    }

    @Test
    void rejectsOrdinaryAccount() {
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "cliente@example.com", "n/a", java.util.List.of());
        assertThat(access.allowed(auth)).isFalse();
        assertThatThrownBy(() -> access.require(auth))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
