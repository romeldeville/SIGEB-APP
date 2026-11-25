package cm.cenc.sigeb.referentiel.config;

import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import cm.cenc.sigeb.referentiel.repository.UtilisateurRepository;

import cm.cenc.sigeb.shared.crypto.service.CryptoService;
import cm.cenc.sigeb.shared.security.model.NiveauEcclesial;
import cm.cenc.sigeb.shared.security.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UtilisateurRepository utilisateurRepository;
    private final CryptoService cryptoService;

    @Value("${sigeb.bootstrap.admin.identifiant:admin@sigeb.cm}")
    private String adminIdentifiant;

    @Value("${sigeb.bootstrap.admin.password:AdminSigeb2026!}")
    private String adminPassword;

    @Value("${sigeb.bootstrap.admin.nom:Secrétariat Général CENC}")
    private String adminNom;

    public BootstrapAdminRunner(UtilisateurRepository utilisateurRepository, CryptoService cryptoService) {
        this.utilisateurRepository = utilisateurRepository;
        this.cryptoService = cryptoService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!utilisateurRepository.existsByRole(Role.ADMIN_NATIONAL)) {
            Utilisateur admin = new Utilisateur();
            admin.setIdentifiant(adminIdentifiant.trim().toLowerCase());
            admin.setMotDePasseHash(cryptoService.hashPassword(adminPassword));
            admin.setNomComplet(adminNom);
            admin.setRole(Role.ADMIN_NATIONAL);
            admin.setNiveau(NiveauEcclesial.NATIONAL);
            admin.setMdpAChanger(true);
            admin.setStatut("ACTIF");
            admin.setLanguePreferee("FR");

            utilisateurRepository.save(admin);
            log.info("Compte Administrateur National initialisé avec succès : {}", adminIdentifiant);
        }
    }
}
