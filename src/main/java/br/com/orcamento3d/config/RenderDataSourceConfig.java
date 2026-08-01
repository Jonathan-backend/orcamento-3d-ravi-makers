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
        URI uri = URI.create(databaseUrl);
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String jdbcUrl = "jdbc:postgresql://%s:%d%s".formatted(uri.getHost(), port, uri.getPath());

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }
}
