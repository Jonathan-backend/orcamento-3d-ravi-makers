package br.com.orcamento3d.budget;

import br.com.orcamento3d.budget.BudgetDtos.*;
import br.com.orcamento3d.customer.*;
import br.com.orcamento3d.gcode.*;
import br.com.orcamento3d.inventory.*;
import br.com.orcamento3d.printer.*;
import br.com.orcamento3d.quote.*;
import br.com.orcamento3d.user.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.*;
import java.util.*;

@Service
public class BudgetService {
 private static final long MAX_GCODE_BYTES=100L*1024*1024;
 private final GcodeAnalyzer analyzer;private final BudgetRepository budgets;private final UserRepository users;
 private final CustomerRepository customers;private final PrinterRepository printers;private final FilamentRepository filaments;
 private final ConsumableRepository consumables;private final PricingConfigRepository pricing;

 public BudgetService(GcodeAnalyzer analyzer,BudgetRepository budgets,UserRepository users,CustomerRepository customers,
  PrinterRepository printers,FilamentRepository filaments,ConsumableRepository consumables,PricingConfigRepository pricing){
  this.analyzer=analyzer;this.budgets=budgets;this.users=users;this.customers=customers;
  this.printers=printers;this.filaments=filaments;this.consumables=consumables;this.pricing=pricing;
 }

 public Analysis analyze(MultipartFile file,String email)throws IOException{
  if(file==null||file.isEmpty())throw new IllegalArgumentException("Selecione um arquivo G-code");
  if(file.getSize()>MAX_GCODE_BYTES)throw new IllegalArgumentException("O G-code deve ter no máximo 100 MB");
  String name=safe(file.getOriginalFilename());String lower=name.toLowerCase();
  if(!(lower.endsWith(".gcode")||lower.endsWith(".gco")||lower.endsWith(".gc")))
   throw new IllegalArgumentException("Formato de G-code inválido");
  GcodeAnalysis a=analyzer.analyze(file.getInputStream());
  List<Printer> ownerPrinters=printers.findByOwnerEmailOrderByName(email);
  Printer matchedPrinter=matchPrinter(ownerPrinters,a.printerModel());
  List<Filament> ownerFilaments=filaments.findByOwnerEmailOrderByBrandAscMaterialAsc(email);
  List<DetectedFilament> detected=a.filaments().stream().map(f->{
   String material=f.material()==null?inferMaterial(name):f.material();
   Filament stock=matchFilament(ownerFilaments,material,f.color());
   return new DetectedFilament(material,f.color(),f.profile(),f.grams(),f.millimeters()/1000.0,
    stock==null?null:stock.getId(),stock==null?null:stock.getBrand()+" "+stock.getMaterial()+" "+stock.getColor());
  }).toList();
  double meters=detected.stream().mapToDouble(DetectedFilament::meters).sum();
  return new Analysis(name,a.lineCount(),a.printTimeMinutes(),a.filamentGrams(),
   meters>0?meters:a.filamentMillimeters()/1000.0,a.magnetInsertionDetected(),a.magnetCount(),detected,
   a.printerModel(),a.printerProfile(),a.slicer(),a.nozzleDiameter(),a.bedWidth(),a.bedDepth(),
   a.printableHeight(),a.modelHeight(),a.layerCount(),a.nozzleTemperature(),a.bedTemperature(),
   matchedPrinter==null?null:matchedPrinter.getId(),matchedPrinter==null?null:matchedPrinter.getName());
 }

 @Transactional public Response create(Request r,String email){return calculate(r,email,true);}
 @Transactional public Response preview(Request r,String email){return calculate(r,email,false);}
 @Transactional public List<ProductionResponse> production(String email){
  return budgets.findByOwnerEmailOrderByCreatedAtDesc(email).stream().map(this::productionResponse).toList();
 }
 @Transactional public ProductionResponse updateProductionStatus(Long id,String status,String email){
  Budget b=budgets.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Orçamento não encontrado"));
  if("DRAFT".equals(b.getStatus()))throw new IllegalArgumentException("Envie o orçamento para produção antes de alterar o status");
  b.setStatus(status);return productionResponse(budgets.save(b));
 }
 @Transactional public Response find(Long id,String email){
  return response(budgets.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Orçamento não encontrado")));
 }
 @Transactional public ProductionResponse update(Long id,ProductionUpdateRequest r,String email){
  Budget b=budgets.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Orçamento não encontrado"));
  b.setTitle(r.title().trim());b.setTotal(money(r.total()));b.setStatus(r.status());
  b.setCustomer(r.customerId()==null?null:customers.findByIdAndOwnerEmail(r.customerId(),email)
   .orElseThrow(()->new IllegalArgumentException("Cliente inválido")));
  return productionResponse(budgets.save(b));
 }
 @Transactional public void delete(Long id,String email){
  Budget b=budgets.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Orçamento não encontrado"));
  budgets.delete(b);
 }
 @Transactional public ProductionResponse duplicate(Long id,String email){
  Budget source=budgets.findByIdAndOwnerEmail(id,email)
   .orElseThrow(()->new NoSuchElementException("Orçamento não encontrado"));
  source.getPlates().size();
  Map<Filament,BigDecimal> filamentUsage=new HashMap<>();
  Map<Consumable,BigDecimal> consumableUsage=new HashMap<>();
  for(BudgetPlate plate:source.getPlates()){
   plate.getFilamentUses().size();
   if(!plate.getFilamentUses().isEmpty()){
    plate.getFilamentUses().forEach(use->
     filamentUsage.merge(use.getFilament(),use.getGrams(),BigDecimal::add));
   }else if(plate.getFilamentGrams()>0){
    if(plate.getFilament()==null)
     throw new IllegalArgumentException("Este orçamento antigo não possui o filamento vinculado e não pode ser duplicado automaticamente");
    filamentUsage.merge(plate.getFilament(),BigDecimal.valueOf(plate.getFilamentGrams()),BigDecimal::add);
   }
   if(plate.getConsumableCost().signum()>0){
    if(plate.getMagnetConsumable()==null||plate.getMagnetQuantity()<=0)
     throw new IllegalArgumentException("Este orçamento antigo possui consumíveis sem vínculo de estoque e precisa ser recriado");
    consumableUsage.merge(plate.getMagnetConsumable(),BigDecimal.valueOf(plate.getMagnetQuantity()),BigDecimal::add);
   }
  }
  filamentUsage.forEach((filament,grams)->{
   if(filament.getWeightGrams().compareTo(grams)<0)
    throw new IllegalArgumentException("Estoque insuficiente de "+filament.getBrand()+" "+filament.getMaterial()+" "+filament.getColor());
  });
  consumableUsage.forEach((consumable,quantity)->{
   if(consumable.getQuantity().compareTo(quantity)<0)
    throw new IllegalArgumentException("Estoque insuficiente de "+consumable.getName());
  });

  Budget copy=new Budget();copy.setOwner(source.getOwner());copy.setCustomer(source.getCustomer());
  copy.setTitle(source.getTitle()+" · Reimpressão");copy.setMarginPercent(source.getMarginPercent());
  copy.setTotal(source.getTotal());copy.setCostTotal(source.getCostTotal());
  copy.setMaintenanceCost(source.getMaintenanceCost());copy.setMachineCost(source.getMachineCost());
  copy.setLaborCost(source.getLaborCost());copy.setAdditionalCost(source.getAdditionalCost());
  copy.setFixedCost(source.getFixedCost());copy.setFailureCost(source.getFailureCost());
  copy.setPurpose(source.getPurpose());copy.setStatus("PRODUCTION");
  for(BudgetPlate original:source.getPlates()){
   BudgetPlate plate=new BudgetPlate();plate.setBudget(copy);plate.setPosition(original.getPosition());
   plate.setName(original.getName());plate.setFileName(original.getFileName());
   plate.setPrintTimeMinutes(original.getPrintTimeMinutes());plate.setFilamentGrams(original.getFilamentGrams());
   plate.setFilamentMeters(original.getFilamentMeters());plate.setPrinter(original.getPrinter());
   plate.setFilament(original.getFilament());plate.setMagnetConsumable(original.getMagnetConsumable());
   plate.setMagnetQuantity(original.getMagnetQuantity());plate.setMaterialCost(original.getMaterialCost());
   plate.setConsumableCost(original.getConsumableCost());plate.setMachineCost(original.getMachineCost());
   plate.setEnergyCost(original.getEnergyCost());plate.setTotal(original.getTotal());copy.getPlates().add(plate);
   original.getFilamentUses().forEach(originalUse->{
    BudgetFilamentUse use=new BudgetFilamentUse();use.setPlate(plate);use.setFilament(originalUse.getFilament());
    use.setGrams(originalUse.getGrams());plate.getFilamentUses().add(use);
   });
  }
  filamentUsage.forEach((filament,grams)->{
   filament.setWeightGrams(filament.getWeightGrams().subtract(grams));filaments.save(filament);
  });
  consumableUsage.forEach((consumable,quantity)->{
   consumable.setQuantity(consumable.getQuantity().subtract(quantity));consumables.save(consumable);
  });
  return productionResponse(budgets.save(copy));
 }
 @Transactional public Budget entity(Long id,String email){
  Budget b=budgets.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("Orçamento não encontrado"));
  b.getPlates().size();if(b.getCustomer()!=null)b.getCustomer().getName();return b;
 }

 private Response calculate(Request r,String email,boolean save){
  PricingConfig p=pricing.findByOwnerEmail(email).orElseGet(()->{
   PricingConfig c=new PricingConfig();c.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());return pricing.save(c);
  });
  Budget b=new Budget();b.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());b.setTitle(r.title().trim());
  b.setCustomer(r.customerId()==null?null:customers.findByIdAndOwnerEmail(r.customerId(),email)
   .orElseThrow(()->new IllegalArgumentException("Cliente inválido")));
  b.setMarginPercent(r.marginPercent());b.setPurpose(r.purpose()==null?"STANDARD_SALE":r.purpose());
  b.setStatus(r.status()==null?"DRAFT":r.status());

  BigDecimal subtotal=BigDecimal.ZERO;BigDecimal machineTotal=BigDecimal.ZERO;
  BigDecimal maintenanceTotal=BigDecimal.ZERO;int pos=0;
  Map<Filament,BigDecimal> filamentUsage=new HashMap<>();
  Map<Consumable,BigDecimal> consumableUsage=new HashMap<>();

  for(PlateRequest item:r.plates()){
   int plateNumber=pos+1;
   if(item.printerId()==null)throw new IllegalArgumentException("Selecione uma impressora na placa "+plateNumber);
   Printer printer=printers.findByIdAndOwnerEmail(item.printerId(),email)
    .orElseThrow(()->new IllegalArgumentException("Impressora inválida na placa "+plateNumber));
   Filament filament=item.filamentId()==null?null:filaments.findByIdAndOwnerEmail(item.filamentId(),email)
    .orElseThrow(()->new IllegalArgumentException("Filamento inválido na placa "+plateNumber));
   BigDecimal hours=BigDecimal.valueOf(item.printTimeMinutes()).divide(BigDecimal.valueOf(60),8,RoundingMode.HALF_UP);
   BigDecimal material=BigDecimal.ZERO;BigDecimal consumableCost=BigDecimal.ZERO;
   Map<Filament,BigDecimal> plateFilamentUsage=new LinkedHashMap<>();
   double totalFilament=item.filamentGrams();

   if(item.filamentUses()!=null&&!item.filamentUses().isEmpty()){
    totalFilament=0;Filament first=null;
    for(FilamentUse use:item.filamentUses()){
     double grams=use.pieceGrams()+use.purgeGrams()+use.towerGrams()+use.supportGrams();
     if(grams<=0)continue;
     Filament selected=use.filamentId()==null?null:filaments.findByIdAndOwnerEmail(use.filamentId(),email)
      .orElseThrow(()->new IllegalArgumentException("Filamento inválido na placa "+plateNumber));
     if(selected==null)throw new IllegalArgumentException("Selecione o filamento do estoque na placa "+plateNumber);
     if(first==null)first=selected;
     material=material.add(selected.getPricePerKg().multiply(BigDecimal.valueOf(grams))
      .divide(BigDecimal.valueOf(1000),8,RoundingMode.HALF_UP));
     totalFilament+=grams;filamentUsage.merge(selected,BigDecimal.valueOf(grams),BigDecimal::add);
     plateFilamentUsage.merge(selected,BigDecimal.valueOf(grams),BigDecimal::add);
    }
    filament=first;
   }else{
    if(totalFilament>0&&filament==null)
     throw new IllegalArgumentException("Selecione o filamento do estoque na placa "+plateNumber);
    if(filament!=null){
     material=filament.getPricePerKg().multiply(BigDecimal.valueOf(totalFilament))
      .divide(BigDecimal.valueOf(1000),8,RoundingMode.HALF_UP);
     filamentUsage.merge(filament,BigDecimal.valueOf(totalFilament),BigDecimal::add);
     plateFilamentUsage.merge(filament,BigDecimal.valueOf(totalFilament),BigDecimal::add);
    }
   }

   Consumable selectedMagnet=null;
   if(item.magnetConsumableId()!=null&&item.magnetQuantity()>0){
    Consumable magnet=consumables.findByIdAndOwnerEmail(item.magnetConsumableId(),email)
     .orElseThrow(()->new IllegalArgumentException("Ímã inválido na placa "+plateNumber));
    if(!"MAGNET".equals(magnet.getCategory()))
     throw new IllegalArgumentException("O consumível selecionado não é um ímã");
    BigDecimal quantity=BigDecimal.valueOf(item.magnetQuantity());
    consumableCost=magnet.getUnitPrice().multiply(quantity);
    material=material.add(consumableCost);consumableUsage.merge(magnet,quantity,BigDecimal::add);
    selectedMagnet=magnet;
   }

   BigDecimal machine=printer.getAcquisitionCost()
    .divide(BigDecimal.valueOf(printer.getUsefulLifeHours()),8,RoundingMode.HALF_UP).multiply(hours);
   BigDecimal plateMaintenance=printer.getMaintenancePerHour().multiply(hours);
   BigDecimal energy=p.getEnergyPricePerKwh().multiply(BigDecimal.valueOf(printer.getPowerWatts()))
    .divide(BigDecimal.valueOf(1000),8,RoundingMode.HALF_UP).multiply(hours);
   BigDecimal plateSubtotal=material.add(machine).add(energy).add(plateMaintenance);
   subtotal=subtotal.add(plateSubtotal);machineTotal=machineTotal.add(machine);
   maintenanceTotal=maintenanceTotal.add(plateMaintenance);

   BudgetPlate plate=new BudgetPlate();plate.setBudget(b);plate.setPosition(++pos);plate.setName(item.name().trim());
   plate.setFileName(clean(item.fileName()));plate.setPrintTimeMinutes(item.printTimeMinutes());
   plate.setFilamentGrams(totalFilament);plate.setFilamentMeters(item.filamentMeters());
   plate.setPrinter(printer);plate.setFilament(filament);plate.setMaterialCost(money(material));
   plate.setMagnetConsumable(selectedMagnet);plate.setMagnetQuantity(item.magnetQuantity());
   plate.setConsumableCost(money(consumableCost));plate.setMachineCost(money(machine));plate.setEnergyCost(money(energy));
   plate.setTotal(money(plateSubtotal.multiply(multiplier(r.marginPercent()))));b.getPlates().add(plate);
   plateFilamentUsage.forEach((usedFilament,grams)->{
    BudgetFilamentUse use=new BudgetFilamentUse();use.setPlate(plate);use.setFilament(usedFilament);
    use.setGrams(grams.setScale(2,RoundingMode.HALF_UP));plate.getFilamentUses().add(use);
   });
  }

  BigDecimal labor=r.postProcessHours().multiply(p.getMachinePricePerHour());
  BigDecimal additional=r.packingCost().add(r.otherCosts());
  BigDecimal fixed=BigDecimal.ZERO;
  BigDecimal directCost=subtotal.add(labor).add(additional).add(fixed);
  BigDecimal failure=directCost.multiply(p.getFailureRatePercent())
   .divide(BigDecimal.valueOf(100),8,RoundingMode.HALF_UP);
  BigDecimal costTotal=directCost.add(failure);
  BigDecimal suggested=costTotal.multiply(multiplier(r.marginPercent()));

  b.setMachineCost(money(machineTotal));b.setLaborCost(money(labor));b.setMaintenanceCost(money(maintenanceTotal));
  b.setAdditionalCost(money(additional));b.setFixedCost(money(fixed));b.setFailureCost(money(failure));b.setCostTotal(money(costTotal));
  b.setTotal(money(r.finalPrice()==null?suggested:r.finalPrice()));

  if(save&&"PRODUCTION".equals(b.getStatus())){
   filamentUsage.forEach((f,grams)->{
    if(f.getWeightGrams().compareTo(grams)<0)
     throw new IllegalArgumentException("Estoque insuficiente de "+f.getBrand()+" "+f.getMaterial()+" "+f.getColor());
    f.setWeightGrams(f.getWeightGrams().subtract(grams));filaments.save(f);
   });
   consumableUsage.forEach((c,quantity)->{
    if(c.getQuantity().compareTo(quantity)<0)throw new IllegalArgumentException("Estoque insuficiente de "+c.getName());
    c.setQuantity(c.getQuantity().subtract(quantity));consumables.save(c);
   });
  }
  return response(save?budgets.save(b):b);
 }

 private Response response(Budget b){
  BigDecimal material=b.getPlates().stream().map(BudgetPlate::getMaterialCost).reduce(BigDecimal.ZERO,BigDecimal::add);
  BigDecimal consumable=b.getPlates().stream().map(BudgetPlate::getConsumableCost).reduce(BigDecimal.ZERO,BigDecimal::add);
  BigDecimal filament=material.subtract(consumable);
  BigDecimal energy=b.getPlates().stream().map(BudgetPlate::getEnergyCost).reduce(BigDecimal.ZERO,BigDecimal::add);
  return new Response(b.getId(),b.getTitle(),b.getCustomer()==null?null:b.getCustomer().getName(),
   b.getCustomer()==null?null:b.getCustomer().getId(),b.getCustomer()==null?null:(b.getCustomer().getWhatsapp()!=null?b.getCustomer().getWhatsapp():b.getCustomer().getPhone()),b.getMarginPercent(),
   filament,consumable,material,energy,b.getMachineCost(),b.getLaborCost(),b.getMaintenanceCost(),b.getAdditionalCost(),
   b.getFixedCost(),b.getFailureCost(),b.getCostTotal(),b.getTotal(),b.getTotal().subtract(b.getCostTotal()),
   b.getPurpose(),b.getStatus(),b.getPlates().stream().map(x->new PlateResponse(x.getId(),x.getName(),x.getFileName(),
    x.getPrintTimeMinutes(),x.getFilamentGrams(),x.getFilamentMeters(),x.getPrinter()==null?null:x.getPrinter().getName(),
    x.getFilament()==null?null:x.getFilament().getBrand()+" "+x.getFilament().getMaterial()+" "+x.getFilament().getColor(),
    x.getMaterialCost(),x.getMachineCost(),x.getEnergyCost(),x.getTotal())).toList(),b.getCreatedAt());
 }

 private ProductionResponse productionResponse(Budget b){
  double grams=b.getPlates().stream().mapToDouble(BudgetPlate::getFilamentGrams).sum();
  double minutes=b.getPlates().stream().mapToDouble(BudgetPlate::getPrintTimeMinutes).sum();
  String printer=b.getPlates().stream().map(BudgetPlate::getPrinter).filter(Objects::nonNull)
   .map(Printer::getName).distinct().reduce((a,c)->a.equals(c)?a:"Múltiplas").orElse(null);
  return new ProductionResponse(b.getId(),b.getTitle(),b.getCustomer()==null?null:b.getCustomer().getName(),
   grams,minutes,b.getCostTotal(),b.getTotal(),b.getStatus(),b.getPlates().size(),printer,b.getCreatedAt());
 }

 private BigDecimal multiplier(BigDecimal margin){
  return BigDecimal.ONE.add(margin.divide(BigDecimal.valueOf(100),8,RoundingMode.HALF_UP));
 }
 private BigDecimal money(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);}
 private String safe(String n){if(n==null)return"arquivo.gcode";String c=n.replace('\\','/');return c.substring(c.lastIndexOf('/')+1);}
 private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
 private String inferMaterial(String name){
  String upper=name.toUpperCase();
  for(String type:List.of("PLA","PETG","ABS","ASA","TPU","NYLON","PC","PVA"))
   if(upper.matches(".*(?:^|[^A-Z])"+type+"(?:[^A-Z]|$).*"))return type;
  return null;
 }
 private Printer matchPrinter(List<Printer> available,String detected){
  if(detected==null||detected.isBlank())return null;String target=normalize(detected);
  return available.stream().filter(Printer::isActive).filter(p->{String model=normalize(p.getModel()),printerName=normalize(p.getName());
   return (!model.isBlank()&&(target.equals(model)||target.contains(model)))||target.equals(printerName)||printerName.contains(target);})
   .findFirst().orElse(null);
 }
 private Filament matchFilament(List<Filament> available,String material,String color){
  return selectFilament(available,normalize(material),normalize(color));
 }
 Filament selectFilament(List<Filament> available,String normalizedMaterial,String normalizedColor){
  List<Filament> sameMaterial=available.stream()
   .filter(f->normalize(f.getMaterial()).equals(normalizedMaterial)).toList();
  if(sameMaterial.isEmpty())return null;
  if(!normalizedColor.isBlank()){
   Optional<Filament> exactInStock=sameMaterial.stream()
    .filter(f->normalize(f.getColor()).equals(normalizedColor))
    .filter(f->f.getWeightGrams()!=null&&f.getWeightGrams().signum()>0).findFirst();
   if(exactInStock.isPresent())return exactInStock.get();
  }
  Optional<Filament> anyInStock=sameMaterial.stream()
   .filter(f->f.getWeightGrams()!=null&&f.getWeightGrams().signum()>0).findFirst();
  if(anyInStock.isPresent())return anyInStock.get();
  if(!normalizedColor.isBlank()){
   Optional<Filament> exact=sameMaterial.stream()
    .filter(f->normalize(f.getColor()).equals(normalizedColor)).findFirst();
   if(exact.isPresent())return exact.get();
  }
  return sameMaterial.get(0);
 }
 private String normalize(String value){return value==null?"":java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD)
  .replaceAll("\\p{M}","").replaceAll("[^a-zA-Z0-9]","").toLowerCase(Locale.ROOT);}
}
