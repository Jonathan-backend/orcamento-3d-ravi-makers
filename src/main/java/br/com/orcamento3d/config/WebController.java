package br.com.orcamento3d.config;

import br.com.orcamento3d.user.Role;
import br.com.orcamento3d.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    private final UserRepository users;
    public WebController(UserRepository users) { this.users = users; }

    @GetMapping("/") public String home() { return "redirect:/entrar"; }
    @GetMapping("/entrar") public String signIn() { return "login"; }
    @GetMapping("/login") public String login() { return "redirect:/entrar"; }
    @GetMapping("/oauth/callback") public String oauthCallback() { return "oauth-callback"; }
    @GetMapping("/register") public String register() { return "redirect:/entrar?mode=register"; }
    @GetMapping("/app") public String app() { return "app"; }
    @GetMapping("/cliente") public String customerPortal() { return "customer-portal"; }
    @GetMapping("/ofertas") public String offers() { return "offers"; }
    @GetMapping("/catalogo/{ownerId}") public String catalog() { return "catalog"; }
}
