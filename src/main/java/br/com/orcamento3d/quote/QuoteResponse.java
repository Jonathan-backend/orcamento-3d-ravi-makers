package br.com.orcamento3d.quote;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponse(Long id, String fileName, long fileSize, long lineCount,
        double printTimeMinutes, double filamentMillimeters, double filamentGrams,
        BigDecimal materialCost, BigDecimal machineCost, BigDecimal energyCost,
        BigDecimal total, BigDecimal profitMarginPercent, String customer, Long customerId,
        Long printerId, String printerName, Instant createdAt) {
    public static QuoteResponse from(Quote q) {
        return new QuoteResponse(q.getId(), q.getFileName(), q.getFileSize(), q.getLineCount(),
                q.getPrintTimeMinutes(), q.getFilamentMillimeters(), q.getFilamentGrams(),
                q.getMaterialCost(), q.getMachineCost(), q.getEnergyCost(), q.getTotal(),
                q.getProfitMarginPercent(),
                q.getCustomer() == null ? q.getOwner().getName() : q.getCustomer().getName(),
                q.getCustomer() == null ? null : q.getCustomer().getId(),
                q.getPrinter() == null ? null : q.getPrinter().getId(),
                q.getPrinter() == null ? null : q.getPrinter().getName(), q.getCreatedAt());
    }
}
