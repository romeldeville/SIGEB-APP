package cm.cenc.sigeb.shared.idempotence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RequeteIdempotenteId implements Serializable {

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Column(name = "cle", nullable = false)
    private UUID cle;

    public RequeteIdempotenteId() {}

    public RequeteIdempotenteId(UUID utilisateurId, UUID cle) {
        this.utilisateurId = utilisateurId;
        this.cle = cle;
    }

    public UUID getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(UUID utilisateurId) { this.utilisateurId = utilisateurId; }

    public UUID getCle() { return cle; }
    public void setCle(UUID cle) { this.cle = cle; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RequeteIdempotenteId that)) return false;
        return Objects.equals(utilisateurId, that.utilisateurId) && Objects.equals(cle, that.cle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(utilisateurId, cle);
    }
}
