package br.com.orcamento3d.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "filament_movements")
public class FilamentMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Filament filament;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deltaGrams;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal resultingWeightGrams;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(length = 200)
    private String note;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Filament getFilament() { return filament; }
    public void setFilament(Filament v) { filament = v; }
    public BigDecimal getDeltaGrams() { return deltaGrams; }
    public void setDeltaGrams(BigDecimal v) { deltaGrams = v; }
    public BigDecimal getResultingWeightGrams() { return resultingWeightGrams; }
    public void setResultingWeightGrams(BigDecimal v) { resultingWeightGrams = v; }
    public String getType() { return type; }
    public void setType(String v) { type = v; }
    public String getNote() { return note; }
    public void setNote(String v) { note = v; }
    public Instant getCreatedAt() { return createdAt; }
}
