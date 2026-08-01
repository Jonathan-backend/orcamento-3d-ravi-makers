package br.com.orcamento3d.quote;

import br.com.orcamento3d.gcode.*;
import br.com.orcamento3d.user.*;
import br.com.orcamento3d.printer.*;
import br.com.orcamento3d.customer.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.*;
import java.util.List;

@Service
public class QuoteService {
    private static final long MAX_GCODE_BYTES = 100L * 1024 * 1024;
    private final GcodeAnalyzer analyzer;
    private final QuoteRepository quotes;
    private final PricingConfigRepository pricing;
    private final UserRepository users;
    private final PrinterRepository printers;
    private final CustomerRepository customers;

    public QuoteService(GcodeAnalyzer analyzer, QuoteRepository quotes,
                        PricingConfigRepository pricing, UserRepository users,
                        PrinterRepository printers, CustomerRepository customers) {
        this.analyzer = analyzer; this.quotes = quotes; this.pricing = pricing; this.users = users;
        this.printers = printers;
        this.customers = customers;
    }

    public Quote create(MultipartFile file, Long printerId, Long customerId,
                        BigDecimal requestedMargin, String email) throws IOException {
        validate(file);
        GcodeAnalysis a = analyzer.analyze(file.getInputStream());
        PricingConfig p = pricing.findByOwnerEmail(email).orElseGet(() -> {
            PricingConfig c=new PricingConfig();c.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());return pricing.save(c);
        });
        Printer selectedPrinter = printerId == null ? null : printers.findByIdAndOwnerEmail(printerId, email)
                .orElseThrow(() -> new IllegalArgumentException("Impressora inválida"));
        Customer selectedCustomer = customerId == null ? null : customers.findByIdAndOwnerEmail(customerId, email)
                .orElseThrow(() -> new IllegalArgumentException("Cliente inválido"));
        BigDecimal margin = requestedMargin == null ? p.getProfitMarginPercent() : requestedMargin;
        if (margin.compareTo(BigDecimal.ZERO) < 0 || margin.compareTo(BigDecimal.valueOf(500)) > 0) {
            throw new IllegalArgumentException("A margem deve estar entre 0% e 500%");
        }
        int powerWatts = selectedPrinter == null ? p.getPrinterPowerWatts() : selectedPrinter.getPowerWatts();
        BigDecimal hours = BigDecimal.valueOf(a.printTimeMinutes() / 60.0);
        BigDecimal material = p.getFilamentPricePerKg()
                .multiply(BigDecimal.valueOf(a.filamentGrams() / 1000.0));
        BigDecimal machine = p.getMachinePricePerHour().multiply(hours);
        BigDecimal energy = p.getEnergyPricePerKwh()
                .multiply(BigDecimal.valueOf(powerWatts / 1000.0)).multiply(hours);
        BigDecimal subtotal = material.add(machine).add(energy);
        BigDecimal total = subtotal.multiply(BigDecimal.ONE.add(
                margin.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));

        Quote q = new Quote();
        q.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());
        q.setPrinter(selectedPrinter);
        q.setCustomer(selectedCustomer);
        q.setProfitMarginPercent(margin);
        q.setFileName(safeName(file.getOriginalFilename()));
        q.setFileSize(file.getSize()); q.setLineCount(a.lineCount());
        q.setPrintTimeMinutes(a.printTimeMinutes()); q.setFilamentMillimeters(a.filamentMillimeters());
        q.setFilamentGrams(a.filamentGrams()); q.setMaterialCost(money(material));
        q.setMachineCost(money(machine)); q.setEnergyCost(money(energy)); q.setTotal(money(total));
        return quotes.save(q);
    }

    public List<Quote> list(String email) {
        return quotes.findByOwnerEmailOrderByCreatedAtDesc(email);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione um arquivo G-code");
        if (file.getSize() > MAX_GCODE_BYTES)
            throw new IllegalArgumentException("O G-code deve ter no máximo 100 MB");
        String name = safeName(file.getOriginalFilename()).toLowerCase();
        if (!(name.endsWith(".gcode") || name.endsWith(".gco") || name.endsWith(".gc"))) {
            throw new IllegalArgumentException("Formato inválido. Envie .gcode, .gco ou .gc");
        }
    }
    private String safeName(String name) {
        if (name == null) return "arquivo.gcode";
        return name.replace("\\", "/").substring(name.replace("\\", "/").lastIndexOf('/') + 1);
    }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
