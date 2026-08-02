package br.com.orcamento3d.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class RenderDataSourceConfig {

    @Bean
    DataSource dataSource(
            @Value("${DB_URL}") String databaseUrl,
            @Value("${DB_USER:}") String username,
            @Value("${DB_PASSWORD:}") String password) {
        DatabaseConnection connection = databaseConnection(databaseUrl, username, password);
        return DataSourceBuilder.create()
                .url(connection.jdbcUrl())
                .username(connection.username())
                .password(connection.password())
                .build();
    }

    static DatabaseConnection databaseConnection(String databaseUrl, String fallbackUsername, String fallbackPassword) {
        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            return new DatabaseConnection(databaseUrl, fallbackUsername, fallbackPassword);
        }

        URI uri = postgresUri(databaseUrl);
        String username = fallbackUsername;
        String password = fallbackPassword;
        if (uri.getRawUserInfo() != null) {
            String[] credentials = uri.getRawUserInfo().split(":", 2);
            username = decode(credentials[0]);
            password = credentials.length == 2 ? decode(credentials[1]) : "";
        }

        return new DatabaseConnection(toJdbcUrl(uri), username, password);
    }

    static String toJdbcUrl(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            return databaseUrl;
        }

        return toJdbcUrl(postgresUri(databaseUrl));
    }

    private static URI postgresUri(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) {
            throw new IllegalArgumentException("DB_URL deve usar jdbc:postgresql://, postgres:// ou postgresql://");
        }
        return uri;
    }

    private static String toJdbcUrl(URI uri) {
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return "jdbc:postgresql://%s:%d%s%s".formatted(uri.getHost(), port, uri.getRawPath(), query);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record DatabaseConnection(String jdbcUrl, String username, String password) {}
}
