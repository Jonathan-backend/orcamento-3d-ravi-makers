package br.com.orcamento3d.customer;
import br.com.orcamento3d.customer.CustomerDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/customers")
public class CustomerController{
 private final CustomerService service;public CustomerController(CustomerService service){this.service=service;}
 @GetMapping public ListResponse list(Authentication a){return service.list(a.getName());}
 @PostMapping public ResponseEntity<Response> create(@Valid @RequestBody Request r,Authentication a){return ResponseEntity.status(201).body(service.create(r,a.getName()));}
 @PutMapping("/{id}") public Response update(@PathVariable Long id,@Valid @RequestBody Request r,Authentication a){return service.update(id,r,a.getName());}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id,Authentication a){service.delete(id,a.getName());}
 @ExceptionHandler(NoSuchElementException.class) ResponseEntity<Map<String,String>> missing(NoSuchElementException e){return ResponseEntity.status(404).body(Map.of("message",e.getMessage()));}
}
