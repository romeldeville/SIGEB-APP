package cm.cenc.sigeb.bapteme.repository;

import cm.cenc.sigeb.bapteme.entity.CompteurIun;
import cm.cenc.sigeb.bapteme.entity.CompteurIunId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompteurIunRepository extends JpaRepository<CompteurIun, CompteurIunId> {

    /**
     * Incrémente de manière atomique le compteur (paroisse_id, annee) grâce à
     * INSERT ... ON CONFLICT DO UPDATE (verrouillage ligne natif PostgreSQL,
     * zéro trou et zéro contention inter-paroisses).
     */
    @Query(value = """
        INSERT INTO compteur_iun (paroisse_id, annee, dernier_numero)
        VALUES (:paroisseId, :annee, 1)
        ON CONFLICT (paroisse_id, annee)
        DO UPDATE SET dernier_numero = compteur_iun.dernier_numero + 1
        RETURNING dernier_numero
        """, nativeQuery = true)
    Integer incrementerEtLire(UUID paroisseId, short annee);
}
