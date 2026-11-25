package cm.cenc.sigeb.bapteme.entity;

import cm.cenc.sigeb.bapteme.enums.GraviteAvenant;
import cm.cenc.sigeb.bapteme.enums.StatutAvenant;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "avenant_rectification")
public class AvenantRectification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acte_bapteme_id", nullable = false)
    private ActeBapteme acteBapteme;

    @Column(name = "version_cible", nullable = false)
    private Integer versionCible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private GraviteAvenant gravite;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anciennes_valeurs_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> anciennesValeurs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelles_valeurs_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> nouvellesValeurs;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(name = "reference_justificatif", length = 180)
    private String referenceJustificatif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatutAvenant statut;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_par_id", nullable = false)
    private Utilisateur demandePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "demande_le", nullable = false, updatable = false)
    private Instant demandeLe = Instant.now();

    @Column(name = "valide_le")
    private Instant valideLe;

    public AvenantRectification() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ActeBapteme getActeBapteme() { return acteBapteme; }
    public void setActeBapteme(ActeBapteme acteBapteme) { this.acteBapteme = acteBapteme; }

    public Integer getVersionCible() { return versionCible; }
    public void setVersionCible(Integer versionCible) { this.versionCible = versionCible; }

    public GraviteAvenant getGravite() { return gravite; }
    public void setGravite(GraviteAvenant gravite) { this.gravite = gravite; }

    public Map<String, Object> getAnciennesValeurs() { return anciennesValeurs; }
    public void setAnciennesValeurs(Map<String, Object> anciennesValeurs) { this.anciennesValeurs = anciennesValeurs; }

    public Map<String, Object> getNouvellesValeurs() { return nouvellesValeurs; }
    public void setNouvellesValeurs(Map<String, Object> nouvellesValeurs) { this.nouvellesValeurs = nouvellesValeurs; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getReferenceJustificatif() { return referenceJustificatif; }
    public void setReferenceJustificatif(String referenceJustificatif) { this.referenceJustificatif = referenceJustificatif; }

    public StatutAvenant getStatut() { return statut; }
    public void setStatut(StatutAvenant statut) { this.statut = statut; }

    public Utilisateur getDemandePar() { return demandePar; }
    public void setDemandePar(Utilisateur demandePar) { this.demandePar = demandePar; }

    public Utilisateur getValidePar() { return validePar; }
    public void setValidePar(Utilisateur validePar) { this.validePar = validePar; }

    public Instant getDemandeLe() { return demandeLe; }
    public void setDemandeLe(Instant demandeLe) { this.demandeLe = demandeLe; }

    public Instant getValideLe() { return valideLe; }
    public void setValideLe(Instant valideLe) { this.valideLe = valideLe; }
}
