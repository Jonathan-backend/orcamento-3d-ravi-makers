package br.com.orcamento3d.quote;

import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class PricingConfig {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="pricing_config_seq")
    @SequenceGenerator(name="pricing_config_seq",sequenceName="pricing_config_seq",allocationSize=1,initialValue=100)
    private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(unique=true)
    private User owner;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal filamentPricePerKg = new BigDecimal("100.00");
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal machinePricePerHour = new BigDecimal("5.00");
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal energyPricePerKwh = new BigDecimal("0.95");
    @Column(nullable = false)
    private int printerPowerWatts = 250;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedCost = new BigDecimal("3.00");
    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal profitMarginPercent = new BigDecimal("30.00");
    @Column(nullable=false,precision=7,scale=2,columnDefinition="numeric(7,2) default 5")
    private BigDecimal failureRatePercent = new BigDecimal("5.00");
    @Column(nullable=false,length=3,columnDefinition="varchar(3) default 'BRL'")
    private String currency = "BRL";

    public Long getId() { return id; }
    public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
    public BigDecimal getFilamentPricePerKg() { return filamentPricePerKg; }
    public void setFilamentPricePerKg(BigDecimal v) { filamentPricePerKg = v; }
    public BigDecimal getMachinePricePerHour() { return machinePricePerHour; }
    public void setMachinePricePerHour(BigDecimal v) { machinePricePerHour = v; }
    public BigDecimal getEnergyPricePerKwh() { return energyPricePerKwh; }
    public void setEnergyPricePerKwh(BigDecimal v) { energyPricePerKwh = v; }
    public int getPrinterPowerWatts() { return printerPowerWatts; }
    public void setPrinterPowerWatts(int v) { printerPowerWatts = v; }
    public BigDecimal getFixedCost() { return fixedCost; }
    public void setFixedCost(BigDecimal v) { fixedCost = v; }
    public BigDecimal getProfitMarginPercent() { return profitMarginPercent; }
    public void setProfitMarginPercent(BigDecimal v) { profitMarginPercent = v; }
    public BigDecimal getFailureRatePercent(){return failureRatePercent;} public void setFailureRatePercent(BigDecimal v){failureRatePercent=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
}
