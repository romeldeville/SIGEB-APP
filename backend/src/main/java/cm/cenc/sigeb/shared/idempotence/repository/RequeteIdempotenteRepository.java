package cm.cenc.sigeb.shared.idempotence.repository;

import cm.cenc.sigeb.shared.idempotence.entity.RequeteIdempotente;
import cm.cenc.sigeb.shared.idempotence.entity.RequeteIdempotenteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RequeteIdempotenteRepository extends JpaRepository<RequeteIdempotente, RequeteIdempotenteId> {
    long deleteByCreeLeBefore(Instant limite);
}
