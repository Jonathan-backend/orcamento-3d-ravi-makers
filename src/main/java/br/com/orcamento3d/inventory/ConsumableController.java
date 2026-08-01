package br.com.orcamento3d.inventory;

import br.com.orcamento3d.user.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/consumables")
public class ConsumableController {
 private final ConsumableRepository items; private final UserRepository users;
 public ConsumableController(ConsumableRepository items,UserRepository users){this.items=items;this.users=users;}
 public record Request(@NotBlank @Pattern(regexp="MAGNET|GLUE|RESIN|HARDWARE|PACKAGING|PAINT|OTHER") String category,
  @NotBlank @Size(max=120) String name,@Size(max=100) String brand,
  @NotBlank @Pattern(regexp="UNIT|G|KG|ML|L|M") String unit,
  @NotNull @DecimalMin("0") @Digits(integer=11,fraction=3) BigDecimal quantity,
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal unitPrice,
  @Size(max=300) String notes){}
 public record Move(@NotNull @DecimalMin("0.001") @Digits(integer=11,fraction=3) BigDecimal quantity){}
 public record Response(Long id,String category,String name,String brand,String unit,BigDecimal quantity,BigDecimal unitPrice,BigDecimal stockValue,String notes,Instant updatedAt){
  static Response from(Consumable c){return new Response(c.getId(),c.getCategory(),c.getName(),c.getBrand(),c.getUnit(),c.getQuantity(),c.getUnitPrice(),c.getQuantity().multiply(c.getUnitPrice()).setScale(2,RoundingMode.HALF_UP),c.getNotes(),c.getUpdatedAt());}
 }
 @GetMapping public List<Response> list(Authentication a){return items.findByOwnerEmailOrderByCategoryAscNameAsc(a.getName()).stream().map(Response::from).toList();}
 @PostMapping @Transactional public ResponseEntity<Response> create(@Valid @RequestBody Request r,Authentication a){Consumable c=new Consumable();c.setOwner(users.findByEmailIgnoreCase(a.getName()).orElseThrow());apply(c,r);return ResponseEntity.status(201).body(Response.from(items.save(c)));}
 @PutMapping("/{id}") @Transactional public Response update(@PathVariable Long id,@Valid @RequestBody Request r,Authentication a){Consumable c=owned(id,a.getName());apply(c,r);return Response.from(items.save(c));}
 @PostMapping("/{id}/{action}") @Transactional public Response move(@PathVariable Long id,@PathVariable String action,@Valid @RequestBody Move r,Authentication a){Consumable c=owned(id,a.getName());BigDecimal delta="add".equals(action)?r.quantity():r.quantity().negate();if(!Set.of("add","remove").contains(action))throw new IllegalArgumentException("Movimento inválido");if(c.getQuantity().add(delta).signum()<0)throw new IllegalArgumentException("Estoque insuficiente");c.setQuantity(c.getQuantity().add(delta));return Response.from(items.save(c));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void delete(@PathVariable Long id,Authentication a){items.delete(owned(id,a.getName()));}
 private Consumable owned(Long id,String email){return items.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Consumível não encontrado"));}
 private void apply(Consumable c,Request r){c.setCategory(r.category());c.setName(r.name().trim());c.setBrand(clean(r.brand()));c.setUnit(r.unit());c.setQuantity(r.quantity());c.setUnitPrice(r.unitPrice());c.setNotes(clean(r.notes()));}
 private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
 @ExceptionHandler({NoSuchElementException.class}) ResponseEntity<Map<String,String>> notFound(RuntimeException e){return ResponseEntity.status(404).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler({IllegalArgumentException.class}) ResponseEntity<Map<String,String>> bad(RuntimeException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
}
