package br.com.orcamento3d.product;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageSecurityTest {
    private final ProductController controller = new ProductController(null, null, null, null);

    @Test
    void rejectsAttributeInjectionAndMismatchedContent() {
        assertThatThrownBy(() -> validate("data:image/png\" onerror=alert(1),AAAA"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validate("data:image/png;base64," +
                Base64.getEncoder().encodeToString("not-a-png".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsPngWithValidSignature() {
        byte[] pngHeader = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        String image = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngHeader);

        assertThat(validate(image)).isEqualTo(image);
    }

    private String validate(String value) {
        return ReflectionTestUtils.invokeMethod(controller, "validImage", value);
    }
}
