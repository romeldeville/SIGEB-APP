package cm.cenc.sigeb.carte.config;

import cm.cenc.sigeb.carte.service.QrSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapCleSignatureRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapCleSignatureRunner.class);
    private final QrSignatureService qrSignatureService;

    public BootstrapCleSignatureRunner(QrSignatureService qrSignatureService) {
        this.qrSignatureService = qrSignatureService;
    }

    @Override
    public void run(String... args) {
        qrSignatureService.initialiserCleSiAbsente();
        log.info("Clé de signature QR active vérifiée.");
    }
}
