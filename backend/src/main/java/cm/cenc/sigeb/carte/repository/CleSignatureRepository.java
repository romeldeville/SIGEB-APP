package cm.cenc.sigeb.carte.repository;

import cm.cenc.sigeb.carte.entity.CleSignature;
import cm.cenc.sigeb.carte.enums.StatutCleSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CleSignatureRepository extends JpaRepository<CleSignature, String> {
    Optional<CleSignature> findByStatut(StatutCleSignature statut);
}
