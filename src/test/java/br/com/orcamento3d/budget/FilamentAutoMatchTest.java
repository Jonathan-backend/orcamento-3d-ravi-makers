package br.com.orcamento3d.budget;

import br.com.orcamento3d.inventory.Filament;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FilamentAutoMatchTest {
    private final BudgetService service = new BudgetService(
            null, null, null, null, null, null, null, null);

    @Test
    void fallsBackToAnyInStockFilamentOfSameMaterialWhenColorDiffers() {
        Filament emptyWhitePla = filament("PLA", "Branco", "0");
        Filament blackPla = filament("PLA", "Preto", "750");
        Filament whitePetg = filament("PETG", "Branco", "1000");

        Filament selected = service.selectFilament(
                List.of(emptyWhitePla, blackPla, whitePetg), "pla", "branco");

        assertThat(selected).isSameAs(blackPla);
    }

    @Test
    void prefersExactColorWhenItIsAvailableInStock() {
        Filament whitePla = filament("PLA", "Branco", "500");
        Filament blackPla = filament("PLA", "Preto", "750");

        Filament selected = service.selectFilament(List.of(blackPla, whitePla), "pla", "branco");

        assertThat(selected).isSameAs(whitePla);
    }

    private Filament filament(String material, String color, String grams) {
        Filament filament = new Filament();
        filament.setMaterial(material);
        filament.setColor(color);
        filament.setBrand("Teste");
        filament.setWeightGrams(new BigDecimal(grams));
        filament.setPricePerKg(BigDecimal.TEN);
        return filament;
    }
}
