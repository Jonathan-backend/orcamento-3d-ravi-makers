package br.com.orcamento3d.budget;
import br.com.orcamento3d.inventory.Filament;
import br.com.orcamento3d.inventory.Consumable;
import br.com.orcamento3d.printer.Printer;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
@Entity @Table(name="budget_plates")
public class BudgetPlate{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) private Budget budget;
 @ManyToOne(fetch=FetchType.LAZY) private Printer printer;
 @ManyToOne(fetch=FetchType.LAZY) private Filament filament;
 @ManyToOne(fetch=FetchType.LAZY) private Consumable magnetConsumable;
 @Column(nullable=false,columnDefinition="integer default 0") private int magnetQuantity;
 @Column(nullable=false) private int position;
 @Column(nullable=false,length=140) private String name;
 @Column(length=255) private String fileName;
 @Column(nullable=false) private double printTimeMinutes;
 @Column(nullable=false) private double filamentGrams;
 @Column(nullable=false) private double filamentMeters;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal materialCost;
 @Column(nullable=false,precision=12,scale=2,columnDefinition="numeric(12,2) default 0") private BigDecimal consumableCost=BigDecimal.ZERO;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal machineCost;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal energyCost;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
 @OneToMany(mappedBy="plate",cascade=CascadeType.ALL,orphanRemoval=true)
 private List<BudgetFilamentUse> filamentUses=new ArrayList<>();
 public Long getId(){return id;} public void setBudget(Budget v){budget=v;}
 public Printer getPrinter(){return printer;} public void setPrinter(Printer v){printer=v;}
 public Filament getFilament(){return filament;} public void setFilament(Filament v){filament=v;}
 public Consumable getMagnetConsumable(){return magnetConsumable;} public void setMagnetConsumable(Consumable v){magnetConsumable=v;}
 public int getMagnetQuantity(){return magnetQuantity;} public void setMagnetQuantity(int v){magnetQuantity=v;}
 public int getPosition(){return position;} public void setPosition(int v){position=v;}
 public String getName(){return name;} public void setName(String v){name=v;}
 public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
 public double getPrintTimeMinutes(){return printTimeMinutes;} public void setPrintTimeMinutes(double v){printTimeMinutes=v;}
 public double getFilamentGrams(){return filamentGrams;} public void setFilamentGrams(double v){filamentGrams=v;}
 public double getFilamentMeters(){return filamentMeters;} public void setFilamentMeters(double v){filamentMeters=v;}
 public BigDecimal getMaterialCost(){return materialCost;} public void setMaterialCost(BigDecimal v){materialCost=v;}
 public BigDecimal getConsumableCost(){return consumableCost;} public void setConsumableCost(BigDecimal v){consumableCost=v;}
 public BigDecimal getMachineCost(){return machineCost;} public void setMachineCost(BigDecimal v){machineCost=v;}
 public BigDecimal getEnergyCost(){return energyCost;} public void setEnergyCost(BigDecimal v){energyCost=v;}
 public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;}
 public List<BudgetFilamentUse> getFilamentUses(){return filamentUses;}
}
