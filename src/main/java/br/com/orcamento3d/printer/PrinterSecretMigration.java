package br.com.orcamento3d.printer;

import br.com.orcamento3d.config.SecretCipher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PrinterSecretMigration implements ApplicationRunner {
    private final PrinterRepository printers;
    private final SecretCipher cipher;

    public PrinterSecretMigration(PrinterRepository printers, SecretCipher cipher) {
        this.printers = printers;
        this.cipher = cipher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printers.findAll().stream()
                .filter(printer -> printer.getApiKey() != null && !printer.getApiKey().isBlank())
                .filter(printer -> !cipher.isEncrypted(printer.getApiKey()))
                .forEach(printer -> printer.setApiKey(cipher.encrypt(printer.getApiKey())));
    }
}
