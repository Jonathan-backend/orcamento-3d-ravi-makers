package br.com.orcamento3d.inventory;

import br.com.orcamento3d.inventory.InventoryDtos.*;
import br.com.orcamento3d.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class InventoryService {
    private final FilamentRepository filaments;
    private final FilamentMovementRepository movements;
    private final UserRepository users;

    public InventoryService(FilamentRepository filaments, FilamentMovementRepository movements,
                            UserRepository users) {
        this.filaments=filaments;this.movements=movements;this.users=users;
    }

    public InventoryResponse list(String email) {
        List<FilamentResponse> items=filaments.findByOwnerEmailOrderByBrandAscMaterialAsc(email)
                .stream().map(FilamentResponse::from).toList();
        BigDecimal grams=items.stream().map(FilamentResponse::weightGrams)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal value=items.stream().map(FilamentResponse::stockValue)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        return new InventoryResponse(items.size(),grams.divide(BigDecimal.valueOf(1000)).doubleValue(),value,items);
    }

    @Transactional
    public FilamentResponse create(FilamentRequest r,String email) {
        Filament f=new Filament();f.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());
        apply(f,r);f=filaments.save(f);
        if(f.getWeightGrams().signum()>0) record(f,f.getWeightGrams(),"ENTRADA","Estoque inicial");
        return FilamentResponse.from(f);
    }

    @Transactional
    public FilamentResponse update(Long id,FilamentRequest r,String email) {
        Filament f=owned(id,email);BigDecimal before=f.getWeightGrams();apply(f,r);f=filaments.save(f);
        BigDecimal delta=f.getWeightGrams().subtract(before);
        if(delta.signum()!=0) record(f,delta,"AJUSTE","Ajuste realizado na edição");
        return FilamentResponse.from(f);
    }

    @Transactional
    public FilamentResponse move(Long id,MovementRequest r,boolean addition,String email) {
        Filament f=owned(id,email);BigDecimal delta=addition?r.grams():r.grams().negate();
        if(f.getWeightGrams().add(delta).signum()<0)
            throw new IllegalArgumentException("Estoque insuficiente para esta retirada");
        f.setWeightGrams(f.getWeightGrams().add(delta));filaments.save(f);
        record(f,delta,addition?"ENTRADA":"RETIRADA",clean(r.note()));
        return FilamentResponse.from(f);
    }

    public List<MovementResponse> history(Long id,String email) {
        owned(id,email);
        return movements.findByFilamentIdOrderByCreatedAtDesc(id).stream()
                .map(MovementResponse::from).toList();
    }

    @Transactional
    public void delete(Long id,String email) {
        Filament f=owned(id,email);movements.deleteByFilamentId(id);filaments.delete(f);
    }

    private Filament owned(Long id,String email) {
        return filaments.findByIdAndOwnerEmail(id,email)
                .orElseThrow(()->new NoSuchElementException("Filamento não encontrado"));
    }

    private void apply(Filament f,FilamentRequest r) {
        f.setMaterial(r.material().trim());f.setColor(r.color().trim());f.setBrand(r.brand().trim());
        f.setWeightGrams(r.weightGrams());f.setPricePerKg(r.pricePerKg());
    }

    private void record(Filament f,BigDecimal delta,String type,String note) {
        FilamentMovement m=new FilamentMovement();m.setFilament(f);m.setDeltaGrams(delta);
        m.setResultingWeightGrams(f.getWeightGrams());m.setType(type);m.setNote(note);movements.save(m);
    }

    private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
}
