package br.com.orcamento3d.printer;

import br.com.orcamento3d.config.SecretCipher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class PrinterSecretMigration {
    private final ObjectProvider<PrinterRepository> printers;
    private final SecretCipher cipher;

    public PrinterSecretMigration(ObjectProvider<PrinterRepository> printers, SecretCipher cipher) {
        this.printers = printers;
        this.cipher = cipher;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrate() {
        printers.getObject().findAll().stream()
                .filter(printer -> printer.getApiKey() != null && !printer.getApiKey().isBlank())
                .filter(printer -> !cipher.isEncrypted(printer.getApiKey()))
                .forEach(printer -> printer.setApiKey(cipher.encrypt(printer.getApiKey())));
    }
}
