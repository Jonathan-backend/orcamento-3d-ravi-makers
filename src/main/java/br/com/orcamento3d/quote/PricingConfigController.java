package br.com.orcamento3d.quote;

import br.com.orcamento3d.user.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/settings/pricing")
public class PricingConfigController {
 private final PricingConfigRepository configs;private final UserRepository users;
 public PricingConfigController(PricingConfigRepository configs,UserRepository users){this.configs=configs;this.users=users;}
 public record Data(
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") BigDecimal energyPricePerKwh,
  @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") BigDecimal laborPricePerHour,
  @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal failureRatePercent,
  @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal defaultMarginPercent,
  @NotBlank @Pattern(regexp="BRL") String currency){}
 @GetMapping @Transactional public Data get(Authentication a){return from(config(a.getName()));}
 @PutMapping @Transactional public Data update(@Valid @RequestBody Data d,Authentication a){PricingConfig p=config(a.getName());p.setEnergyPricePerKwh(d.energyPricePerKwh());p.setMachinePricePerHour(d.laborPricePerHour());p.setFailureRatePercent(d.failureRatePercent());p.setProfitMarginPercent(d.defaultMarginPercent());p.setFixedCost(BigDecimal.ZERO);p.setCurrency(d.currency());return from(configs.save(p));}
 private PricingConfig config(String email){return configs.findByOwnerEmail(email).orElseGet(()->{PricingConfig p=new PricingConfig();p.setOwner(users.findByEmailIgnoreCase(email).orElseThrow());return configs.save(p);});}
 private Data from(PricingConfig p){return new Data(p.getEnergyPricePerKwh(),p.getMachinePricePerHour(),p.getFailureRatePercent(),p.getProfitMarginPercent(),p.getCurrency());}
}
