package br.com.orcamento3d.auth;

import br.com.orcamento3d.user.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final boolean secureCookie;
    public OAuthSuccessHandler(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                               @Value("${app.auth.secure-cookie:false}") boolean secureCookie) {
        this.users=users;this.encoder=encoder;this.jwt=jwt;this.secureCookie=secureCookie;
    }
    @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)throws IOException{
        OAuth2User principal=(OAuth2User)authentication.getPrincipal();
        Object emailAttribute=principal.getAttribute("email");
        Object nameAttribute=principal.getAttribute("name");
        if(emailAttribute==null||emailAttribute.toString().isBlank()){
            response.sendRedirect("/entrar?oauth_error=email");return;
        }
        String email=emailAttribute.toString().trim().toLowerCase();
        String name=nameAttribute==null||nameAttribute.toString().isBlank()?email:nameAttribute.toString().trim();
        User user=users.findByEmailIgnoreCase(email).orElseGet(()->{
            User created=new User();created.setEmail(email);created.setName(name);
            created.setPassword(encoder.encode(UUID.randomUUID().toString()+UUID.randomUUID()));
            created.setRole(Role.ADMIN);return users.save(created);
        });
        if(user.getRole()!=Role.ADMIN||user.getAccountOwner()!=null){
            response.sendRedirect("/entrar?oauth_error=role");return;
        }
        if(!user.isEnabled()){response.sendRedirect("/entrar?oauth_error=disabled");return;}
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("ORCA_AUTH",jwt.generate(user))
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/")
                .maxAge(jwt.getExpirationSeconds()).build().toString());
        String fragment="name="+encode(user.getName())+"&email="+encode(user.getEmail())+"&role="+user.getRole().name();
        HttpSession oauthSession=request.getSession(false);
        if(oauthSession!=null)oauthSession.invalidate();
        response.sendRedirect("/oauth/callback#"+fragment);
    }
    private String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
}
