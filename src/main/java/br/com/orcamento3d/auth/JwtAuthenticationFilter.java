package br.com.orcamento3d.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import br.com.orcamento3d.user.UserRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, UserRepository users) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : cookieToken(request);
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String username = jwtService.extractUsername(token);
            UserDetails details = userDetailsService.loadUserByUsername(username);
            if (jwtService.isValid(token, details)) {
                var account = users.findByEmailIgnoreCase(username).orElseThrow();
                String operationalEmail = account.effectiveOwner().getEmail();
                UserDetails operationalDetails = org.springframework.security.core.userdetails.User
                        .withUsername(operationalEmail).password(details.getPassword())
                        .authorities(details.getAuthorities()).disabled(!details.isEnabled()).build();
                var auth = new UsernamePasswordAuthenticationToken(operationalDetails, null, details.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Requisições da API com Bearer válido sempre usam os papéis do JWT,
                // mesmo quando ainda existe uma sessão OAuth do Google no navegador.
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | UsernameNotFoundException ignored) {
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }

    private String cookieToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> "ORCA_AUTH".equals(cookie.getName()))
                .map(Cookie::getValue).findFirst().orElse(null);
    }
}
