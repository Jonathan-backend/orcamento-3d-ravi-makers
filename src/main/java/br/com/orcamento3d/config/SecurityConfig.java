package br.com.orcamento3d.config;

import br.com.orcamento3d.auth.JwtAuthenticationFilter;
import br.com.orcamento3d.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    UserDetailsService userDetailsService(ObjectProvider<UserRepository> users) {
        return email -> users.getObject().findByEmailIgnoreCase(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail()).password(user.getPassword())
                        .roles(user.getRole().name()).disabled(!user.isEnabled()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwt,
                                    WebSecurityFilter webSecurityFilter,
                                    org.springframework.beans.factory.ObjectProvider<ClientRegistrationRepository> clients,
                                    br.com.orcamento3d.auth.OAuthSuccessHandler oauthSuccess) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/entrar", "/login", "/register", "/catalogo/**", "/css/**", "/js/**", "/img/**",
                                "/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/public/**", "/error").permitAll()
                        .requestMatchers("/api/settings/**", "/api/printers/**", "/api/feedback/**",
                                "/api/support/**").hasRole("ADMIN")
                        .requestMatchers("/api/team/**").denyAll()
                        .requestMatchers("/api/inventory/**", "/api/consumables/**", "/api/customers/**",
                                "/api/budgets/**", "/api/quotes/**", "/api/products/**", "/api/coupons/**")
                                .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> request.getRequestURI().startsWith("/api/")))
                .addFilterBefore(webSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        if (clients.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/entrar")
                    .successHandler(oauthSuccess)
                    .failureUrl("/entrar?oauth_error=1"));
        }
        return http.build();
    }
}
