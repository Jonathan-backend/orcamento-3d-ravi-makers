package br.com.orcamento3d.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Locale;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionSecurityValidator implements ApplicationRunner {
    // Uma chave aleatória de 256 bits em Base64 tem 43 ou 44 caracteres.
    private static final int MINIMUM_RANDOM_SECRET_LENGTH = 43;
    private static final Set<String> UNSAFE_VALUES = Set.of(
            "troque-esta-chave-em-producao-com-pelo-menos-32-bytes",
            "gere-uma-chave-aleatoria-segura-com-ao-menos-32-bytes",
            "change-me", "secret", "password"
    );

    private final Environment environment;
    private final String jwtSecret;
    private final String encryptionSecret;

    public ProductionSecurityValidator(Environment environment, @Value("${app.jwt.secret}") String jwtSecret,
                                       @Value("${app.encryption.secret}") String encryptionSecret) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.encryptionSecret = encryptionSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.matchesProfiles("prod")) return;
        String normalized = jwtSecret == null ? "" : jwtSecret.trim().toLowerCase(Locale.ROOT);
        if (jwtSecret == null || jwtSecret.length() < MINIMUM_RANDOM_SECRET_LENGTH
                || UNSAFE_VALUES.contains(normalized)) {
            throw new IllegalStateException(
                    "JWT_SECRET inseguro. Em produção, configure um segredo aleatório de pelo menos 256 bits.");
        }
        String normalizedEncryption = encryptionSecret == null ? "" : encryptionSecret.trim().toLowerCase(Locale.ROOT);
        if (encryptionSecret == null || encryptionSecret.length() < MINIMUM_RANDOM_SECRET_LENGTH
                || UNSAFE_VALUES.contains(normalizedEncryption)
                || encryptionSecret.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "DATA_ENCRYPTION_KEY insegura. Configure uma chave aleatória diferente do JWT_SECRET.");
        }
    }
}
