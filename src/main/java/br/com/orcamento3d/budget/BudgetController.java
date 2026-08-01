package br.com.orcamento3d.budget;
import br.com.orcamento3d.budget.BudgetDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.List;
@RestController @RequestMapping("/api/budgets")
public class BudgetController{
 private final BudgetService service;private final BudgetPdfService pdf;
 public BudgetController(BudgetService service,BudgetPdfService pdf){this.service=service;this.pdf=pdf;}
 @PostMapping(value="/analyze",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Analysis analyze(@RequestPart("file")MultipartFile file,Authentication a)throws IOException{return service.analyze(file,a.getName());}
 @PostMapping public ResponseEntity<Response> create(@Valid @RequestBody Request r,Authentication a){return ResponseEntity.status(201).body(service.create(r,a.getName()));}
 @PostMapping("/preview") public Response preview(@Valid @RequestBody Request r,Authentication a){return service.preview(r,a.getName());}
 @GetMapping("/production") public List<ProductionResponse> production(Authentication a){return service.production(a.getName());}
 @GetMapping("/{id}") public Response get(@PathVariable Long id,Authentication a){return service.find(id,a.getName());}
 @PutMapping("/{id}") public ProductionResponse update(@PathVariable Long id,@Valid @RequestBody ProductionUpdateRequest r,Authentication a){return service.update(id,r,a.getName());}
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id,Authentication a){service.delete(id,a.getName());return ResponseEntity.noContent().build();}
 @PostMapping("/{id}/duplicate") public ResponseEntity<ProductionResponse> duplicate(@PathVariable Long id,Authentication a){
  return ResponseEntity.status(201).body(service.duplicate(id,a.getName()));
 }
 @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
 public ResponseEntity<byte[]> pdf(@PathVariable Long id,Authentication a)throws IOException{
  byte[] bytes=pdf.generate(id,a.getName());
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"orcamento-"+id+"-ravi-makers.pdf\"").body(bytes);
 }
 @PatchMapping("/{id}/status") public ProductionResponse status(@PathVariable Long id,@Valid @RequestBody ProductionStatusRequest r,Authentication a){return service.updateProductionStatus(id,r.status(),a.getName());}
 @ExceptionHandler({IllegalArgumentException.class}) ResponseEntity<Map<String,String>> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("message",e.getMessage()));}
 @ExceptionHandler({java.util.NoSuchElementException.class}) ResponseEntity<Map<String,String>> missing(java.util.NoSuchElementException e){return ResponseEntity.status(404).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(IOException.class) ResponseEntity<Map<String,String>> invalid(){return ResponseEntity.unprocessableEntity().body(Map.of("message","Não foi possível analisar o G-code"));}
}
