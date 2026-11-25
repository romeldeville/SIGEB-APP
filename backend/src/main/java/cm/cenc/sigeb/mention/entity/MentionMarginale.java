package cm.cenc.sigeb.mention.entity;

import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.mention.enums.StatutMention;
import cm.cenc.sigeb.mention.enums.TypeSacrement;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "mention_marginale")
public class MentionMarginale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acte_bapteme_id", nullable = false)
    private ActeBapteme acteBapteme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_declarante_id", nullable = false)
    private Paroisse paroisseDeclarante;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sacrement", nullable = false, length = 30)
    private TypeSacrement typeSacrement;

    @Column(name = "date_evenement", nullable = false)
    private LocalDate dateEvenement;

    @Column(name = "lieu_evenement", nullable = false, length = 160)
    private String lieuEvenement;

    @Column(name = "ministre_celebrant", nullable = false, length = 160)
    private String ministreCelebrant;

    @Column(name = "conjoint_nom_complet", length = 180)
    private String conjointNomComplet;

    @Column(name = "ref_registre_local", length = 80)
    private String refRegistreLocal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatutMention statut = StatutMention.EN_ATTENTE_ORIGINE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "declare_par_id", nullable = false)
    private Utilisateur declarePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "declare_le", nullable = false, updatable = false)
    private Instant declareLe = Instant.now();

    @Column(name = "valide_le")
    private Instant valideLe;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    public MentionMarginale() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ActeBapteme getActeBapteme() { return acteBapteme; }
    public void setActeBapteme(ActeBapteme acteBapteme) { this.acteBapteme = acteBapteme; }

    public Paroisse getParoisseDeclarante() { return paroisseDeclarante; }
    public void setParoisseDeclarante(Paroisse paroisseDeclarante) { this.paroisseDeclarante = paroisseDeclarante; }

    public TypeSacrement getTypeSacrement() { return typeSacrement; }
    public void setTypeSacrement(TypeSacrement typeSacrement) { this.typeSacrement = typeSacrement; }

    public LocalDate getDateEvenement() { return dateEvenement; }
    public void setDateEvenement(LocalDate dateEvenement) { this.dateEvenement = dateEvenement; }

    public String getLieuEvenement() { return lieuEvenement; }
    public void setLieuEvenement(String lieuEvenement) { this.lieuEvenement = lieuEvenement; }

    public String getMinistreCelebrant() { return ministreCelebrant; }
    public void setMinistreCelebrant(String ministreCelebrant) { this.ministreCelebrant = ministreCelebrant; }

    public String getConjointNomComplet() { return conjointNomComplet; }
    public void setConjointNomComplet(String conjointNomComplet) { this.conjointNomComplet = conjointNomComplet; }

    public String getRefRegistreLocal() { return refRegistreLocal; }
    public void setRefRegistreLocal(String refRegistreLocal) { this.refRegistreLocal = refRegistreLocal; }

    public StatutMention getStatut() { return statut; }
    public void setStatut(StatutMention statut) { this.statut = statut; }

    public Utilisateur getDeclarePar() { return declarePar; }
    public void setDeclarePar(Utilisateur declarePar) { this.declarePar = declarePar; }

    public Utilisateur getValidePar() { return validePar; }
    public void setValidePar(Utilisateur validePar) { this.validePar = validePar; }

    public Instant getDeclareLe() { return declareLe; }
    public void setDeclareLe(Instant declareLe) { this.declareLe = declareLe; }

    public Instant getValideLe() { return valideLe; }
    public void setValideLe(Instant valideLe) { this.valideLe = valideLe; }

    public String getMotifRejet() { return motifRejet; }
    public void setMotifRejet(String motifRejet) { this.motifRejet = motifRejet; }
}
