package br.com.orcamento3d.inventory;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class InventoryDtos {
    private InventoryDtos() {}
    public record FilamentRequest(
            @NotBlank @Size(max=60) String material,
            @NotBlank @Size(max=60) String color,
            @NotBlank @Size(max=100) String brand,
            @NotNull @DecimalMin("0.00") @DecimalMax("1000000.00") @Digits(integer=7,fraction=2) BigDecimal weightGrams,
            @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal pricePerKg) {}
    public record MovementRequest(@NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer=7,fraction=2) BigDecimal grams, @Size(max=200) String note) {}
    public record MovementResponse(Long id, BigDecimal deltaGrams, BigDecimal resultingWeightGrams,
                                   String type, String note, Instant createdAt) {
        static MovementResponse from(FilamentMovement m) {
            return new MovementResponse(m.getId(),m.getDeltaGrams(),m.getResultingWeightGrams(),
                    m.getType(),m.getNote(),m.getCreatedAt());
        }
    }
    public record FilamentResponse(Long id,String material,String color,String brand,BigDecimal weightGrams,
                                   BigDecimal pricePerKg,BigDecimal stockValue,Instant updatedAt) {
        static FilamentResponse from(Filament f) {
            BigDecimal value=f.getPricePerKg().multiply(f.getWeightGrams())
                    .divide(BigDecimal.valueOf(1000),2,java.math.RoundingMode.HALF_UP);
            return new FilamentResponse(f.getId(),f.getMaterial(),f.getColor(),f.getBrand(),
                    f.getWeightGrams(),f.getPricePerKg(),value,f.getUpdatedAt());
        }
    }
    public record InventoryResponse(int registered,double totalKg,BigDecimal totalValue,
                                    List<FilamentResponse> filaments) {}
}
