package cm.cenc.sigeb.carte.entity;

import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.carte.enums.*;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "carte_emise")
public class CarteEmise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acte_bapteme_id", nullable = false)
    private ActeBapteme acteBapteme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_emettrice_id", nullable = false)
    private Paroisse paroisseEmettrice;

    @Column(name = "code_carte", nullable = false, unique = true, length = 30)
    private String codeCarte;

    @Enumerated(EnumType.STRING)
    @Column(name = "format_document", nullable = false, length = 20)
    private FormatDocument formatDocument;

    @Column(nullable = false, length = 2)
    private String langue;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_emission", nullable = false, length = 15)
    private TypeEmission typeEmission;

    @Column(name = "numero_duplicata", nullable = false)
    private short numeroDuplicata = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_emission", nullable = false, length = 30)
    private MotifEmission motifEmission;

    @Column(name = "version_acte_au_tirage", nullable = false)
    private Integer versionActeAuTirage;

    @Column(name = "nb_mentions_au_tirage", nullable = false)
    private short nbMentionsAuTirage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees_imprimees_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> donneesImprimees;

    @Column(name = "empreinte_donnees", nullable = false, length = 64)
    private String empreinteDonnees;

    @Column(name = "template_version", nullable = false, length = 10)
    private String templateVersion;

    @Column(name = "qr_token", nullable = false, unique = true, length = 64)
    private String qrToken;

    @Column(name = "qr_kid", nullable = false, length = 16)
    private String qrKid;

    @Column(name = "qr_signature", nullable = false, length = 128)
    private String qrSignature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCarte statut = StatutCarte.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remplacee_par_carte_id")
    private CarteEmise remplaceeParCarte;

    @Column(name = "revoquee_le")
    private Instant revoqueeLe;

    @Column(name = "motif_revocation", length = 180)
    private String motifRevocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emis_par_id", nullable = false)
    private Utilisateur emisPar;

    @Column(name = "emis_le", nullable = false, updatable = false)
    private Instant emisLe = Instant.now();

    public CarteEmise() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ActeBapteme getActeBapteme() { return acteBapteme; }
    public void setActeBapteme(ActeBapteme acteBapteme) { this.acteBapteme = acteBapteme; }

    public Paroisse getParoisseEmettrice() { return paroisseEmettrice; }
    public void setParoisseEmettrice(Paroisse paroisseEmettrice) { this.paroisseEmettrice = paroisseEmettrice; }

    public String getCodeCarte() { return codeCarte; }
    public void setCodeCarte(String codeCarte) { this.codeCarte = codeCarte; }

    public FormatDocument getFormatDocument() { return formatDocument; }
    public void setFormatDocument(FormatDocument formatDocument) { this.formatDocument = formatDocument; }

    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }

    public TypeEmission getTypeEmission() { return typeEmission; }
    public void setTypeEmission(TypeEmission typeEmission) { this.typeEmission = typeEmission; }

    public short getNumeroDuplicata() { return numeroDuplicata; }
    public void setNumeroDuplicata(short numeroDuplicata) { this.numeroDuplicata = numeroDuplicata; }

    public MotifEmission getMotifEmission() { return motifEmission; }
    public void setMotifEmission(MotifEmission motifEmission) { this.motifEmission = motifEmission; }

    public Integer getVersionActeAuTirage() { return versionActeAuTirage; }
    public void setVersionActeAuTirage(Integer versionActeAuTirage) { this.versionActeAuTirage = versionActeAuTirage; }

    public short getNbMentionsAuTirage() { return nbMentionsAuTirage; }
    public void setNbMentionsAuTirage(short nbMentionsAuTirage) { this.nbMentionsAuTirage = nbMentionsAuTirage; }

    public Map<String, Object> getDonneesImprimees() { return donneesImprimees; }
    public void setDonneesImprimees(Map<String, Object> donneesImprimees) { this.donneesImprimees = donneesImprimees; }

    public String getEmpreinteDonnees() { return empreinteDonnees; }
    public void setEmpreinteDonnees(String empreinteDonnees) { this.empreinteDonnees = empreinteDonnees; }

    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public String getQrKid() { return qrKid; }
    public void setQrKid(String qrKid) { this.qrKid = qrKid; }

    public String getQrSignature() { return qrSignature; }
    public void setQrSignature(String qrSignature) { this.qrSignature = qrSignature; }

    public StatutCarte getStatut() { return statut; }
    public void setStatut(StatutCarte statut) { this.statut = statut; }

    public CarteEmise getRemplaceeParCarte() { return remplaceeParCarte; }
    public void setRemplaceeParCarte(CarteEmise remplaceeParCarte) { this.remplaceeParCarte = remplaceeParCarte; }

    public Instant getRevoqueeLe() { return revoqueeLe; }
    public void setRevoqueeLe(Instant revoqueeLe) { this.revoqueeLe = revoqueeLe; }

    public String getMotifRevocation() { return motifRevocation; }
    public void setMotifRevocation(String motifRevocation) { this.motifRevocation = motifRevocation; }

    public Utilisateur getEmisPar() { return emisPar; }
    public void setEmisPar(Utilisateur emisPar) { this.emisPar = emisPar; }

    public Instant getEmisLe() { return emisLe; }
    public void setEmisLe(Instant emisLe) { this.emisLe = emisLe; }
}
