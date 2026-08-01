package br.com.orcamento3d.feedback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SupportAccess {
    private final Set<String> admins;

    public SupportAccess(@Value("${app.support.admin-emails:}") String configuredEmails) {
        admins = Arrays.stream(configuredEmails.split(","))
                .map(String::trim).map(String::toLowerCase).filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean allowed(Authentication authentication) {
        return authentication != null && admins.contains(authentication.getName().trim().toLowerCase());
    }

    public void require(Authentication authentication) {
        if (!allowed(authentication)) throw new AccessDeniedException("Acesso exclusivo do administrador geral.");
    }
}
