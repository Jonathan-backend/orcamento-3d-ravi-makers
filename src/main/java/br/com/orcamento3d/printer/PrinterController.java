package br.com.orcamento3d.printer;

import br.com.orcamento3d.printer.PrinterDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/printers")
public class PrinterController {
    private final PrinterService service;
    public PrinterController(PrinterService service) { this.service = service; }

    @GetMapping
    public List<Response> list(Authentication auth) { return service.list(auth.getName()); }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Request request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request, Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @GetMapping("/{id}/status")
    public Response status(@PathVariable Long id, Authentication auth) {
        return service.status(id, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth.getName());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String,String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
