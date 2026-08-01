package br.com.orcamento3d.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityFilterTest {
    private final WebSecurityFilter filter = new WebSecurityFilter();

    @Test
    void addsBrowserSecurityHeaders() throws Exception {
        var request = new MockHttpServletRequest("GET", "/entrar");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Content-Security-Policy"))
                .contains("frame-ancestors 'none'")
                .contains("img-src 'self' data: blob:");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void blocksCrossSiteApiMutation() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/logout");
        request.addHeader("Origin", "https://evil.example");
        request.addHeader("Sec-Fetch-Site", "cross-site");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("bloqueada");
    }

    @Test
    void blocksCookieMutationWithoutBrowserOriginSignals() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/auth/logout");
        request.setCookies(new Cookie("ORCA_AUTH", "token"));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsBearerApiClientWithoutBrowserOriginSignals() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/budgets/preview");
        request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
