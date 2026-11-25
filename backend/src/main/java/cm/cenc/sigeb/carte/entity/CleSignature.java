package cm.cenc.sigeb.carte.entity;

import cm.cenc.sigeb.carte.enums.StatutCleSignature;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cle_signature")
public class CleSignature {

    @Id
    @Column(length = 16)
    private String kid;

    @Column(name = "secret_chiffre", nullable = false)
    private byte[] secretChiffre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatutCleSignature statut;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    @Column(name = "retiree_le")
    private Instant retireeLe;

    public CleSignature() {}

    public String getKid() { return kid; }
    public void setKid(String kid) { this.kid = kid; }

    public byte[] getSecretChiffre() { return secretChiffre; }
    public void setSecretChiffre(byte[] secretChiffre) { this.secretChiffre = secretChiffre; }

    public StatutCleSignature getStatut() { return statut; }
    public void setStatut(StatutCleSignature statut) { this.statut = statut; }

    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }

    public Instant getRetireeLe() { return retireeLe; }
    public void setRetireeLe(Instant retireeLe) { this.retireeLe = retireeLe; }
}
