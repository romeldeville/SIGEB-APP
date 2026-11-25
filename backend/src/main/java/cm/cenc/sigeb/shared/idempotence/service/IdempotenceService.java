package cm.cenc.sigeb.shared.idempotence.service;

import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.idempotence.entity.RequeteIdempotente;
import cm.cenc.sigeb.shared.idempotence.entity.RequeteIdempotenteId;
import cm.cenc.sigeb.shared.idempotence.repository.RequeteIdempotenteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotenceService {

    private final RequeteIdempotenteRepository repository;

    public IdempotenceService(RequeteIdempotenteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<RequeteIdempotente> verifier(UUID utilisateurId, UUID cle, String empreinteRequete) {
        Optional<RequeteIdempotente> existing = repository.findById(new RequeteIdempotenteId(utilisateurId, cle));
        if (existing.isPresent() && !existing.get().getEmpreinteRequete().equals(empreinteRequete)) {
            throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                    "Cette clé Idempotency-Key a déjà été utilisée avec un contenu de requête différent");
        }
        return existing;
    }

    @Transactional
    public void memoriser(UUID utilisateurId, UUID cle, String empreinteRequete, int statutHttp, Map<String, Object> reponseJson) {
        RequeteIdempotente req = new RequeteIdempotente();
        req.setId(new RequeteIdempotenteId(utilisateurId, cle));
        req.setEmpreinteRequete(empreinteRequete);
        req.setStatutHttp((short) statutHttp);
        req.setReponseJson(reponseJson);
        repository.save(req);
    }

    @Transactional
    public long purgerAnciennes() {
        return repository.deleteByCreeLeBefore(Instant.now().minus(48, ChronoUnit.HOURS));
    }
}
