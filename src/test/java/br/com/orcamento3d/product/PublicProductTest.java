package br.com.orcamento3d.product;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PublicProductTest {
    @Test
    void publicProductDoesNotExposeInternalCostsOrMargins() throws Exception {
        Product product = new Product();
        product.setName("Peça");
        product.setDescription("Descrição");
        product.setPrice(new BigDecimal("49.90"));
        product.setTechnicalCost(new BigDecimal("12.00"));
        product.setCategory("Decoração");

        String json = new ObjectMapper().writeValueAsString(ProductController.PublicProduct.from(product));

        assertThat(json).contains("\"price\":49.90");
        assertThat(json).doesNotContain("technicalCost", "marginValue", "marginPercent");
    }
}
