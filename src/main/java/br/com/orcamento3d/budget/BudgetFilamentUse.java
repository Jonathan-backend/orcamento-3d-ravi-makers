package br.com.orcamento3d.budget;

import br.com.orcamento3d.inventory.Filament;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "budget_filament_uses")
public class BudgetFilamentUse {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional = false, fetch = FetchType.LAZY) private BudgetPlate plate;
 @ManyToOne(optional = false, fetch = FetchType.LAZY) private Filament filament;
 @Column(nullable = false, precision = 12, scale = 2) private BigDecimal grams;
 public Long getId(){return id;}
 public BudgetPlate getPlate(){return plate;} public void setPlate(BudgetPlate v){plate=v;}
 public Filament getFilament(){return filament;} public void setFilament(Filament v){filament=v;}
 public BigDecimal getGrams(){return grams;} public void setGrams(BigDecimal v){grams=v;}
}
