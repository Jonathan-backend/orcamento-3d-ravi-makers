package br.com.orcamento3d.printer;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class PrinterDtos {
    private PrinterDtos() {}

    public record Request(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 100) String manufacturer,
            @Size(max = 100) String model,
            @Min(1) @Max(20000) int powerWatts,
            @PositiveOrZero @DecimalMax("9999999999.99") @Digits(integer=10, fraction=2) BigDecimal acquisitionCost,
            @Min(1) @Max(1000000) int usefulLifeHours,
            @PositiveOrZero @DecimalMax("9999999999.99") @Digits(integer=10, fraction=2) BigDecimal maintenancePerHour,
            @Size(max = 1000) String notes,
            @NotNull PrinterType type,
            @Size(max = 300) String baseUrl,
            @Size(max = 500) String apiKey,
            boolean monitoringEnabled,
            boolean active) {}

    public record Status(String code, String label, String fileName,
                         Double progress, Long secondsRemaining, String detail) {
        public static Status manual() {
            return new Status("MANUAL", "Status manual", null, null, null,
                    "Esta impressora não possui monitoramento automático configurado");
        }
    }

    public record Response(Long id, String name, String manufacturer, String model,
                           int powerWatts, BigDecimal acquisitionCost, int usefulLifeHours,
                           BigDecimal maintenancePerHour, String notes, PrinterType type, String baseUrl,
                           boolean hasApiKey, boolean monitoringEnabled, boolean active,
                           Status status) {
        public static Response from(Printer p, Status status) {
            return new Response(p.getId(), p.getName(), p.getManufacturer(), p.getModel(),
                    p.getPowerWatts(), p.getAcquisitionCost(), p.getUsefulLifeHours(),
                    p.getMaintenancePerHour(), p.getNotes(), p.getType(), p.getBaseUrl(),
                    p.getApiKey() != null && !p.getApiKey().isBlank(),
                    p.isMonitoringEnabled(), p.isActive(), status);
        }
    }
}
