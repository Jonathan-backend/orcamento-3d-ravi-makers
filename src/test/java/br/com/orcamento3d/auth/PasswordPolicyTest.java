package br.com.orcamento3d.auth;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {
    @Test
    void rejectsWeakPasswordAndAcceptsLetterNumberCombination() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var weak = new AuthDtos.RegisterRequest("Teste", "teste@example.com", "abcdefghij");
            var strong = new AuthDtos.RegisterRequest("Teste", "teste@example.com", "segura2026x");

            assertThat(validator.validate(weak)).isNotEmpty();
            assertThat(validator.validate(strong)).isEmpty();
        }
    }
}
