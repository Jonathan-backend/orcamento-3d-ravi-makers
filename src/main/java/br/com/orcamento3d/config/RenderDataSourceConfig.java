package br.com.orcamento3d.config;

import java.net.URI;

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
            @Value("${DB_USER}") String username,
            @Value("${DB_PASSWORD}") String password) {
        return DataSourceBuilder.create()
                .url(toJdbcUrl(databaseUrl))
                .username(username)
                .password(password)
                .build();
    }

    static String toJdbcUrl(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            return databaseUrl;
        }

        URI uri = URI.create(databaseUrl);
        if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) {
            throw new IllegalArgumentException("DB_URL deve usar jdbc:postgresql://, postgres:// ou postgresql://");
        }

        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return "jdbc:postgresql://%s:%d%s%s".formatted(uri.getHost(), port, uri.getRawPath(), query);
    }
}
