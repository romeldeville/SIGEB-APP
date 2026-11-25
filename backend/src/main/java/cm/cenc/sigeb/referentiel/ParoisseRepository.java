package cm.cenc.sigeb.referentiel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParoisseRepository extends JpaRepository<Paroisse, UUID> {
    Optional<Paroisse> findByDioceseIdAndCode(UUID dioceseId, String code);
    List<Paroisse> findByDioceseId(UUID dioceseId);
}
