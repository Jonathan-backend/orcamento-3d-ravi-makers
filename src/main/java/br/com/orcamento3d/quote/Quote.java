package br.com.orcamento3d.quote;

import br.com.orcamento3d.user.User;
import br.com.orcamento3d.printer.Printer;
import br.com.orcamento3d.customer.Customer;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Quote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User owner;
    @ManyToOne(fetch = FetchType.LAZY)
    private Printer printer;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @Column(nullable = false)
    private String fileName;
    private long fileSize;
    private long lineCount;
    private double printTimeMinutes;
    private double filamentMillimeters;
    private double filamentGrams;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal materialCost;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal machineCost;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal energyCost;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Column(nullable = false, precision = 7, scale = 2,
            columnDefinition = "numeric(7,2) default 0")
    private BigDecimal profitMarginPercent = BigDecimal.ZERO;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User v) { owner = v; }
    public Printer getPrinter() { return printer; }
    public void setPrinter(Printer v) { printer = v; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer v) { customer = v; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { fileName = v; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long v) { fileSize = v; }
    public long getLineCount() { return lineCount; }
    public void setLineCount(long v) { lineCount = v; }
    public double getPrintTimeMinutes() { return printTimeMinutes; }
    public void setPrintTimeMinutes(double v) { printTimeMinutes = v; }
    public double getFilamentMillimeters() { return filamentMillimeters; }
    public void setFilamentMillimeters(double v) { filamentMillimeters = v; }
    public double getFilamentGrams() { return filamentGrams; }
    public void setFilamentGrams(double v) { filamentGrams = v; }
    public BigDecimal getMaterialCost() { return materialCost; }
    public void setMaterialCost(BigDecimal v) { materialCost = v; }
    public BigDecimal getMachineCost() { return machineCost; }
    public void setMachineCost(BigDecimal v) { machineCost = v; }
    public BigDecimal getEnergyCost() { return energyCost; }
    public void setEnergyCost(BigDecimal v) { energyCost = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { total = v; }
    public BigDecimal getProfitMarginPercent() { return profitMarginPercent; }
    public void setProfitMarginPercent(BigDecimal v) { profitMarginPercent = v; }
    public Instant getCreatedAt() { return createdAt; }
}
