package cm.cenc.sigeb.bapteme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CompteurIunId implements Serializable {

    @Column(name = "paroisse_id", nullable = false)
    private UUID paroisseId;

    @Column(name = "annee", nullable = false)
    private Short annee;

    public CompteurIunId() {}

    public CompteurIunId(UUID paroisseId, Short annee) {
        this.paroisseId = paroisseId;
        this.annee = annee;
    }

    public UUID getParoisseId() { return paroisseId; }
    public void setParoisseId(UUID paroisseId) { this.paroisseId = paroisseId; }

    public Short getAnnee() { return annee; }
    public void setAnnee(Short annee) { this.annee = annee; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompteurIunId that)) return false;
        return Objects.equals(paroisseId, that.paroisseId) && Objects.equals(annee, that.annee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paroisseId, annee);
    }
}
