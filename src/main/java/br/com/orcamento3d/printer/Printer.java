package br.com.orcamento3d.printer;

import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "printers")
public class Printer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User owner;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(length = 100)
    private String manufacturer;
    @Column(length = 100)
    private String model;
    @Column(nullable = false)
    private int powerWatts;
    @Column(nullable = false, precision = 12, scale = 2, columnDefinition = "numeric(12,2) default 0")
    private BigDecimal acquisitionCost = BigDecimal.ZERO;
    @Column(nullable = false, columnDefinition = "integer default 10000")
    private int usefulLifeHours = 10000;
    @Column(nullable = false, precision = 10, scale = 2, columnDefinition = "numeric(10,2) default 0")
    private BigDecimal maintenancePerHour = BigDecimal.ZERO;
    @Column(length = 1000)
    private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PrinterType type = PrinterType.MANUAL;
    @Column(length = 300)
    private String baseUrl;
    @Column(length = 1000)
    private String apiKey;
    @Column(nullable = false)
    private boolean monitoringEnabled;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User v) { owner = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String v) { manufacturer = v; }
    public String getModel() { return model; }
    public void setModel(String v) { model = v; }
    public int getPowerWatts() { return powerWatts; }
    public void setPowerWatts(int v) { powerWatts = v; }
    public BigDecimal getAcquisitionCost() { return acquisitionCost; }
    public void setAcquisitionCost(BigDecimal v) { acquisitionCost = v; }
    public int getUsefulLifeHours() { return usefulLifeHours; }
    public void setUsefulLifeHours(int v) { usefulLifeHours = v; }
    public BigDecimal getMaintenancePerHour() { return maintenancePerHour; }
    public void setMaintenancePerHour(BigDecimal v) { maintenancePerHour = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public PrinterType getType() { return type; }
    public void setType(PrinterType v) { type = v; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String v) { baseUrl = v; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String v) { apiKey = v; }
    public boolean isMonitoringEnabled() { return monitoringEnabled; }
    public void setMonitoringEnabled(boolean v) { monitoringEnabled = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public Instant getCreatedAt() { return createdAt; }
}
