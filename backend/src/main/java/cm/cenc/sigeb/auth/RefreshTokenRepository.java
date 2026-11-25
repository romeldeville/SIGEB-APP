package cm.cenc.sigeb.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoqueLe = :maintenant WHERE r.familleId = :familleId AND r.revoqueLe IS NULL")
    void revoquerFamille(UUID familleId, Instant maintenant);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoqueLe = :maintenant WHERE r.utilisateur.id = :utilisateurId AND r.revoqueLe IS NULL")
    void revoquerTousPourUtilisateur(UUID utilisateurId, Instant maintenant);
}
