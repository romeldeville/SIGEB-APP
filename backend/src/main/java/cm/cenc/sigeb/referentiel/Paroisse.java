package cm.cenc.sigeb.referentiel;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "paroisse", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"diocese_id", "code"})
})
public class Paroisse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diocese_id", nullable = false)
    private Diocese diocese;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "type_paroisse", nullable = false, length = 20)
    private String typeParoisse; // PAROISSE, QUASI_PAROISSE, MISSION

    @Column(name = "nom_fr", nullable = false, length = 160)
    private String nomFr;

    @Column(name = "nom_en", nullable = false, length = 160)
    private String nomEn;

    @Column(nullable = false, length = 120)
    private String localite;

    @Column(name = "entete_officiel", length = 255)
    private String enteteOfficiel;

    @Column(nullable = false)
    private boolean actif = true;

    public Paroisse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Diocese getDiocese() { return diocese; }
    public void setDiocese(Diocese diocese) { this.diocese = diocese; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTypeParoisse() { return typeParoisse; }
    public void setTypeParoisse(String typeParoisse) { this.typeParoisse = typeParoisse; }

    public String getNomFr() { return nomFr; }
    public void setNomFr(String nomFr) { this.nomFr = nomFr; }

    public String getNomEn() { return nomEn; }
    public void setNomEn(String nomEn) { this.nomEn = nomEn; }

    public String getLocalite() { return localite; }
    public void setLocalite(String localite) { this.localite = localite; }

    public String getEnteteOfficiel() { return enteteOfficiel; }
    public void setEnteteOfficiel(String enteteOfficiel) { this.enteteOfficiel = enteteOfficiel; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
