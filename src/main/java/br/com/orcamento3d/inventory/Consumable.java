package br.com.orcamento3d.inventory;

import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name="consumables")
public class Consumable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) private User owner;
    @Column(nullable=false,length=30) private String category;
    @Column(nullable=false,length=120) private String name;
    @Column(length=100) private String brand;
    @Column(nullable=false,length=20) private String unit;
    @Column(nullable=false,precision=14,scale=3) private BigDecimal quantity=BigDecimal.ZERO;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal unitPrice=BigDecimal.ZERO;
    @Column(length=300) private String notes;
    @Column(nullable=false) private Instant updatedAt=Instant.now();
    @PreUpdate void touch(){updatedAt=Instant.now();}
    public Long getId(){return id;} public User getOwner(){return owner;} public void setOwner(User v){owner=v;}
    public String getCategory(){return category;} public void setCategory(String v){category=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getBrand(){return brand;} public void setBrand(String v){brand=v;}
    public String getUnit(){return unit;} public void setUnit(String v){unit=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public BigDecimal getUnitPrice(){return unitPrice;} public void setUnitPrice(BigDecimal v){unitPrice=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public Instant getUpdatedAt(){return updatedAt;}
}
