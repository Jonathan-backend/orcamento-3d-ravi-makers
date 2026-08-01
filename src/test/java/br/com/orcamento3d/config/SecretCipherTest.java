package br.com.orcamento3d.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCipherTest {
    @Test
    void encryptsWithAuthenticatedCipherAndDecrypts() {
        SecretCipher cipher = new SecretCipher(
                "chave-de-teste-distinta-com-mais-de-quarenta-e-oito-caracteres");

        String encrypted = cipher.encrypt("octoprint-secret");

        assertThat(encrypted).startsWith("enc:v1:").doesNotContain("octoprint-secret");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("octoprint-secret");
        assertThat(cipher.encrypt("octoprint-secret")).isNotEqualTo(encrypted);
    }
}
