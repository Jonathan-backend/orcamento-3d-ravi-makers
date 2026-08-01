package br.com.orcamento3d.quote;

import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {
    private final QuoteService service;
    public QuoteController(QuoteService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@RequestPart("file") MultipartFile file,
                                    @RequestParam(required = false) Long printerId,
                                    @RequestParam(required = false) Long customerId,
                                    @RequestParam(required = false) java.math.BigDecimal profitMarginPercent,
                                    Authentication auth) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "message","Calculador antigo desativado. Use Orçar & Produzir para aplicar estoque, impressora e parâmetros atuais."));
    }

    @GetMapping
    public List<QuoteResponse> list(Authentication auth) {
        return service.list(auth.getName()).stream().map(QuoteResponse::from).toList();
    }
}
