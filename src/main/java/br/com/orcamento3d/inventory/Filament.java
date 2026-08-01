package br.com.orcamento3d.inventory;

import br.com.orcamento3d.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "filaments")
public class Filament {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User owner;
    @Column(nullable = false, length = 60)
    private String material;
    @Column(nullable = false, length = 60)
    private String color;
    @Column(nullable = false, length = 100)
    private String brand;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal weightGrams = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerKg;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User v) { owner = v; }
    public String getMaterial() { return material; }
    public void setMaterial(String v) { material = v; }
    public String getColor() { return color; }
    public void setColor(String v) { color = v; }
    public String getBrand() { return brand; }
    public void setBrand(String v) { brand = v; }
    public BigDecimal getWeightGrams() { return weightGrams; }
    public void setWeightGrams(BigDecimal v) { weightGrams = v; }
    public BigDecimal getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(BigDecimal v) { pricePerKg = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
