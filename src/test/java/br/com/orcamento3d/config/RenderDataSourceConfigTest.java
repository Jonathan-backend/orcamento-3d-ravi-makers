package br.com.orcamento3d.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RenderDataSourceConfigTest {

    @Test
    void keepsJdbcUrlUnchanged() {
        String url = "jdbc:postgresql://db.example.com:5432/app?sslmode=require";
        assertEquals(url, RenderDataSourceConfig.toJdbcUrl(url));
    }

    @Test
    void convertsProviderPostgresUrlAndKeepsQuery() {
        assertEquals(
                "jdbc:postgresql://db.example.com:6432/app?sslmode=require",
                RenderDataSourceConfig.toJdbcUrl("postgresql://user:pass@db.example.com:6432/app?sslmode=require"));
    }

    @Test
    void rejectsUnsupportedSchemes() {
        assertThrows(IllegalArgumentException.class,
                () -> RenderDataSourceConfig.toJdbcUrl("mysql://db.example.com/app"));
    }
}
