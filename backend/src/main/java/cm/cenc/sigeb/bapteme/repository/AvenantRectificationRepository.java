package cm.cenc.sigeb.bapteme.repository;

import cm.cenc.sigeb.bapteme.entity.AvenantRectification;
import cm.cenc.sigeb.bapteme.enums.StatutAvenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AvenantRectificationRepository extends JpaRepository<AvenantRectification, UUID> {
    List<AvenantRectification> findByActeBaptemeIdOrderByDemandeLeDesc(UUID acteBaptemeId);
    List<AvenantRectification> findByStatutAndActeBaptemeParoisseDioceseIdOrderByDemandeLeDesc(StatutAvenant statut, UUID dioceseId);
    boolean existsByActeBaptemeIdAndStatut(UUID acteBaptemeId, StatutAvenant statut);
}
