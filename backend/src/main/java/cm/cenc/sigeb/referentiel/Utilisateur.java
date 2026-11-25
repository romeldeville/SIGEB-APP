package cm.cenc.sigeb.referentiel;

import cm.cenc.sigeb.shared.security.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String identifiant;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @Column(name = "pin_signature_hash", length = 255)
    private String pinSignatureHash;

    @Column(name = "totp_secret_chiffre")
    private byte[] totpSecretChiffre;

    @Column(name = "totp_active", nullable = false)
    private boolean totpActive = false;

    @Column(name = "mdp_a_changer", nullable = false)
    private boolean mdpAChanger = true;

    @Column(name = "nom_complet", nullable = false, length = 160)
    private String nomComplet;

    @Column(length = 30)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diocese_id")
    private Diocese diocese;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id")
    private Paroisse paroisse;

    @Column(name = "langue_preferee", nullable = false, length = 2)
    private String languePreferee = "FR";

    @Column(nullable = false, length = 20)
    private String statut = "ACTIF"; // ACTIF, SUSPENDU

    @Column(name = "tentatives_echec", nullable = false)
    private short tentativesEchec = 0;

    @Column(name = "verrouille_jusqua")
    private Instant verrouilleJusqua;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    public Utilisateur() {}

    public boolean isVerrouille() {
        return verrouilleJusqua != null && verrouilleJusqua.isAfter(Instant.now());
    }

    public boolean isActif() {
        return "ACTIF".equals(statut) && !isVerrouille();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) { this.motDePasseHash = motDePasseHash; }

    public String getPinSignatureHash() { return pinSignatureHash; }
    public void setPinSignatureHash(String pinSignatureHash) { this.pinSignatureHash = pinSignatureHash; }

    public byte[] getTotpSecretChiffre() { return totpSecretChiffre; }
    public void setTotpSecretChiffre(byte[] totpSecretChiffre) { this.totpSecretChiffre = totpSecretChiffre; }

    public boolean isTotpActive() { return totpActive; }
    public void setTotpActive(boolean totpActive) { this.totpActive = totpActive; }

    public boolean isMdpAChanger() { return mdpAChanger; }
    public void setMdpAChanger(boolean mdpAChanger) { this.mdpAChanger = mdpAChanger; }

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Diocese getDiocese() { return diocese; }
    public void setDiocese(Diocese diocese) { this.diocese = diocese; }

    public Paroisse getParoisse() { return paroisse; }
    public void setParoisse(Paroisse paroisse) { this.paroisse = paroisse; }

    public String getLanguePreferee() { return languePreferee; }
    public void setLanguePreferee(String languePreferee) { this.languePreferee = languePreferee; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public short getTentativesEchec() { return tentativesEchec; }
    public void setTentativesEchec(short tentativesEchec) { this.tentativesEchec = tentativesEchec; }

    public Instant getVerrouilleJusqua() { return verrouilleJusqua; }
    public void setVerrouilleJusqua(Instant verrouilleJusqua) { this.verrouilleJusqua = verrouilleJusqua; }

    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }
}
