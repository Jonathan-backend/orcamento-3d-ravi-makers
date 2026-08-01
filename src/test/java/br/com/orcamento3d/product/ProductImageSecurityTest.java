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

    @Test
    void acceptsTenMegabytesAndRejectsAnythingLarger() {
        byte[] allowed = new byte[10_000_000];
        allowed[0] = (byte) 137; allowed[1] = 80; allowed[2] = 78; allowed[3] = 71;
        allowed[4] = 13; allowed[5] = 10; allowed[6] = 26; allowed[7] = 10;
        String allowedImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(allowed);
        assertThat(validate(allowedImage)).isEqualTo(allowedImage);

        byte[] oversized = new byte[10_000_001];
        System.arraycopy(allowed, 0, oversized, 0, allowed.length);
        String oversizedImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(oversized);
        assertThatThrownBy(() -> validate(oversizedImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB");
    }

    private String validate(String value) {
        return ReflectionTestUtils.invokeMethod(controller, "validImage", value);
    }
}
