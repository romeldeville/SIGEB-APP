package cm.cenc.sigeb.auth;

import cm.cenc.sigeb.referentiel.Utilisateur;
import cm.cenc.sigeb.referentiel.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class LoginAttemptService {

    private final UtilisateurRepository utilisateurRepository;

    public LoginAttemptService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrerEchec(UUID userId) {
        Utilisateur user = utilisateurRepository.findById(userId).orElseThrow();
        short echecs = (short) (user.getTentativesEchec() + 1);
        user.setTentativesEchec(echecs);
        if (echecs >= 5) {
            long minutesLock = echecs >= 10 ? 60 : 15;
            user.setVerrouilleJusqua(Instant.now().plus(minutesLock, ChronoUnit.MINUTES));
        }
        utilisateurRepository.save(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reinitialiserEchecs(UUID userId) {
        Utilisateur user = utilisateurRepository.findById(userId).orElseThrow();
        if (user.getTentativesEchec() > 0 || user.getVerrouilleJusqua() != null) {
            user.setTentativesEchec((short) 0);
            user.setVerrouilleJusqua(null);
            utilisateurRepository.save(user);
        }
    }
}
