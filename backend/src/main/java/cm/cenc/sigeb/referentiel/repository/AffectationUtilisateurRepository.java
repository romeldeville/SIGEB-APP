package cm.cenc.sigeb.referentiel.repository;

import cm.cenc.sigeb.referentiel.entity.AffectationUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AffectationUtilisateurRepository extends JpaRepository<AffectationUtilisateur, UUID> {
    Optional<AffectationUtilisateur> findByUtilisateurIdAndDateFinIsNull(UUID utilisateurId);
    List<AffectationUtilisateur> findByUtilisateurIdOrderByDateDebutDesc(UUID utilisateurId);
}
