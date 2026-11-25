package cm.cenc.sigeb.auth.entity;

import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "famille_id", nullable = false)
    private UUID familleId;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "revoque_le")
    private Instant revoqueLe;

    public RefreshToken() {}

    public RefreshToken(Utilisateur utilisateur, String tokenHash, UUID familleId, Instant expireLe) {
        this.utilisateur = utilisateur;
        this.tokenHash = tokenHash;
        this.familleId = familleId;
        this.expireLe = expireLe;
    }

    public boolean isActif() {
        return revoqueLe == null && expireLe.isAfter(Instant.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public UUID getFamilleId() { return familleId; }
    public void setFamilleId(UUID familleId) { this.familleId = familleId; }

    public Instant getExpireLe() { return expireLe; }
    public void setExpireLe(Instant expireLe) { this.expireLe = expireLe; }

    public Instant getRevoqueLe() { return revoqueLe; }
    public void setRevoqueLe(Instant revoqueLe) { this.revoqueLe = revoqueLe; }
}
