package br.com.orcamento3d.inventory;

import br.com.orcamento3d.inventory.InventoryDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service){this.service=service;}
    @GetMapping public InventoryResponse list(Authentication a){return service.list(a.getName());}
    @PostMapping public ResponseEntity<FilamentResponse> create(@Valid @RequestBody FilamentRequest r,Authentication a){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r,a.getName()));
    }
    @PutMapping("/{id}") public FilamentResponse update(@PathVariable Long id,@Valid @RequestBody FilamentRequest r,Authentication a){
        return service.update(id,r,a.getName());
    }
    @PostMapping("/{id}/add") public FilamentResponse add(@PathVariable Long id,@Valid @RequestBody MovementRequest r,Authentication a){
        return service.move(id,r,true,a.getName());
    }
    @PostMapping("/{id}/remove") public FilamentResponse remove(@PathVariable Long id,@Valid @RequestBody MovementRequest r,Authentication a){
        return service.move(id,r,false,a.getName());
    }
    @GetMapping("/{id}/history") public List<MovementResponse> history(@PathVariable Long id,Authentication a){
        return service.history(id,a.getName());
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,Authentication a){service.delete(id,a.getName());}
    @ExceptionHandler({NoSuchElementException.class})
    ResponseEntity<Map<String,String>> notFound(RuntimeException e){return ResponseEntity.status(404).body(Map.of("message",e.getMessage()));}
    @ExceptionHandler({IllegalArgumentException.class})
    ResponseEntity<Map<String,String>> bad(RuntimeException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
}
