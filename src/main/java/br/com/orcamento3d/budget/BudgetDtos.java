package br.com.orcamento3d.budget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
public final class BudgetDtos{
 private BudgetDtos(){}
 public record FilamentUse(Long filamentId,@Size(max=60) String color,
  @PositiveOrZero double pieceGrams,@PositiveOrZero double purgeGrams,
  @PositiveOrZero double towerGrams,@PositiveOrZero double supportGrams){}
 public record PlateRequest(@NotBlank @Size(max=140) String name,@Size(max=255) String fileName,
  @PositiveOrZero double printTimeMinutes,@PositiveOrZero double filamentGrams,
  @PositiveOrZero double filamentMeters,Long printerId,Long filamentId,
  @Size(max=16) List<@Valid FilamentUse> filamentUses,Long magnetConsumableId,@PositiveOrZero int magnetQuantity){}
 public record Request(@NotBlank @Size(max=140) String title,Long customerId,
 @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal marginPercent,
  @DecimalMin("0") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal finalPrice,@Pattern(regexp="STANDARD_SALE|WHOLESALE|INTERNAL|SAMPLE") String purpose,
  @NotNull @DecimalMin("0") BigDecimal postProcessHours,
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal packingCost,
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal otherCosts,
  @Pattern(regexp="DRAFT|PRODUCTION") String status,
  @NotEmpty @Size(max=20) List<@Valid PlateRequest> plates){}
 public record DetectedFilament(String material,String color,String profile,double grams,double meters,Long stockFilamentId,String stockFilamentName){}
 public record Analysis(String fileName,long lineCount,double printTimeMinutes,double filamentGrams,double filamentMeters,
  boolean magnetInsertionDetected,int magnetCount,List<DetectedFilament> filaments,
  String printerModel,String printerProfile,String slicer,double nozzleDiameter,double bedWidth,double bedDepth,
  double printableHeight,double modelHeight,int layerCount,double nozzleTemperature,double bedTemperature,
  Long matchedPrinterId,String matchedPrinterName){}
 public record PlateResponse(Long id,String name,String fileName,double printTimeMinutes,double filamentGrams,
  double filamentMeters,String printerName,String filamentName,BigDecimal materialCost,BigDecimal machineCost,
  BigDecimal energyCost,BigDecimal total){}
 public record Response(Long id,String title,String customer,Long customerId,String customerWhatsapp,BigDecimal marginPercent,BigDecimal filamentCost,BigDecimal consumableCost,BigDecimal materialCost,
  BigDecimal energyCost,BigDecimal machineCost,BigDecimal laborCost,BigDecimal maintenanceCost,BigDecimal additionalCost,
  BigDecimal fixedCost,BigDecimal failureCost,BigDecimal costTotal,BigDecimal total,BigDecimal profit,String purpose,String status,
  List<PlateResponse> plates,Instant createdAt){}
 public record ProductionStatusRequest(@NotBlank @Pattern(regexp="PRODUCTION|PRINTING|PAUSED|COMPLETED|CANCELLED") String status){}
 public record ProductionUpdateRequest(@NotBlank @Size(max=140) String title,Long customerId,
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") @Digits(integer=10,fraction=2) BigDecimal total,
  @NotBlank @Pattern(regexp="DRAFT|PRODUCTION|PRINTING|PAUSED|COMPLETED|CANCELLED") String status){}
 public record ProductionResponse(Long id,String title,String customer,double filamentGrams,double printTimeMinutes,
  BigDecimal costTotal,BigDecimal total,String status,int plates,String printer,Instant createdAt){}
}
