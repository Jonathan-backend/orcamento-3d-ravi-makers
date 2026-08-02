package br.com.orcamento3d.config;

import br.com.orcamento3d.quote.*;
import br.com.orcamento3d.user.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {
    private final ObjectProvider<UserRepository> users;
    private final ObjectProvider<PricingConfigRepository> pricing;
    private final PasswordEncoder encoder; private final String adminPassword;
    public DataInitializer(ObjectProvider<UserRepository> users,
                           ObjectProvider<PricingConfigRepository> pricing,
                           PasswordEncoder encoder,
                           @Value("${app.admin.initial-password:}") String adminPassword) {
        this.users = users; this.pricing = pricing; this.encoder = encoder; this.adminPassword = adminPassword;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        PricingConfigRepository pricing = this.pricing.getObject();
        UserRepository users = this.users.getObject();
        pricing.findById(1L).orElseGet(() -> pricing.save(new PricingConfig()));
        if (!adminPassword.isBlank() && !users.existsByEmailIgnoreCase("admin@orcamento3d.local")) {
            User admin = new User(); admin.setName("Administrador");
            admin.setEmail("admin@orcamento3d.local"); admin.setPassword(encoder.encode(adminPassword));
            admin.setRole(Role.ADMIN); users.save(admin);
        }
    }
}
