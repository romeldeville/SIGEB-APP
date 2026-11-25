package cm.cenc.sigeb.bapteme.entity;

import cm.cenc.sigeb.bapteme.enums.OrigineActe;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import cm.cenc.sigeb.referentiel.entity.Utilisateur;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "acte_bapteme")
public class ActeBapteme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, length = 40)
    private String iun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paroisse_id", nullable = false)
    private Paroisse paroisse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigineActe origine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatutActe statut = StatutActe.BROUILLON;

    @Version
    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(nullable = false, length = 120)
    private String prenoms;

    @Column(name = "nom_bapteme", length = 120)
    private String nomBapteme;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 1)
    private String sexe;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance", nullable = false, length = 150)
    private String lieuNaissance;

    @Column(name = "nom_pere", length = 160)
    private String nomPere;

    @Column(name = "nom_mere", length = 160)
    private String nomMere;

    @Column(name = "filiation_confidentielle", nullable = false)
    private boolean filiationConfidentielle = false;

    @Column(name = "nom_parrain", length = 160)
    private String nomParrain;

    @Column(name = "nom_marraine", length = 160)
    private String nomMarraine;

    @Column(name = "paroisse_parrainage", length = 160)
    private String paroisseParrainage;

    @Column(name = "date_bapteme", nullable = false)
    private LocalDate dateBapteme;

    @Column(name = "lieu_bapteme", nullable = false, length = 160)
    private String lieuBapteme;

    @Column(name = "ministre_celebrant", nullable = false, length = 160)
    private String ministreCelebrant;

    @Column(name = "registre_tome", nullable = false, length = 20)
    private String registreTome;

    @Column(name = "registre_page", nullable = false, length = 20)
    private String registrePage;

    @Column(name = "registre_numero", nullable = false, length = 30)
    private String registreNumero;

    @Column(name = "motif_correction", columnDefinition = "TEXT")
    private String motifCorrection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saisi_par_id", nullable = false)
    private Utilisateur saisiPar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scelle_par_id")
    private Utilisateur scellePar;

    @Column(name = "scelle_le")
    private Instant scelleLe;

    @Column(name = "empreinte_scellement", length = 64)
    private String empreinteScellement;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe = Instant.now();

    public ActeBapteme() {}

    @PreUpdate
    public void onUpdate() {
        this.modifieLe = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIun() { return iun; }
    public void setIun(String iun) { this.iun = iun; }

    public Paroisse getParoisse() { return paroisse; }
    public void setParoisse(Paroisse paroisse) { this.paroisse = paroisse; }

    public OrigineActe getOrigine() { return origine; }
    public void setOrigine(OrigineActe origine) { this.origine = origine; }

    public StatutActe getStatut() { return statut; }
    public void setStatut(StatutActe statut) { this.statut = statut; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenoms() { return prenoms; }
    public void setPrenoms(String prenoms) { this.prenoms = prenoms; }

    public String getNomBapteme() { return nomBapteme; }
    public void setNomBapteme(String nomBapteme) { this.nomBapteme = nomBapteme; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getLieuNaissance() { return lieuNaissance; }
    public void setLieuNaissance(String lieuNaissance) { this.lieuNaissance = lieuNaissance; }

    public String getNomPere() { return nomPere; }
    public void setNomPere(String nomPere) { this.nomPere = nomPere; }

    public String getNomMere() { return nomMere; }
    public void setNomMere(String nomMere) { this.nomMere = nomMere; }

    public boolean isFiliationConfidentielle() { return filiationConfidentielle; }
    public void setFiliationConfidentielle(boolean filiationConfidentielle) { this.filiationConfidentielle = filiationConfidentielle; }

    public String getNomParrain() { return nomParrain; }
    public void setNomParrain(String nomParrain) { this.nomParrain = nomParrain; }

    public String getNomMarraine() { return nomMarraine; }
    public void setNomMarraine(String nomMarraine) { this.nomMarraine = nomMarraine; }

    public String getParoisseParrainage() { return paroisseParrainage; }
    public void setParoisseParrainage(String paroisseParrainage) { this.paroisseParrainage = paroisseParrainage; }

    public LocalDate getDateBapteme() { return dateBapteme; }
    public void setDateBapteme(LocalDate dateBapteme) { this.dateBapteme = dateBapteme; }

    public String getLieuBapteme() { return lieuBapteme; }
    public void setLieuBapteme(String lieuBapteme) { this.lieuBapteme = lieuBapteme; }

    public String getMinistreCelebrant() { return ministreCelebrant; }
    public void setMinistreCelebrant(String ministreCelebrant) { this.ministreCelebrant = ministreCelebrant; }

    public String getRegistreTome() { return registreTome; }
    public void setRegistreTome(String registreTome) { this.registreTome = registreTome; }

    public String getRegistrePage() { return registrePage; }
    public void setRegistrePage(String registrePage) { this.registrePage = registrePage; }

    public String getRegistreNumero() { return registreNumero; }
    public void setRegistreNumero(String registreNumero) { this.registreNumero = registreNumero; }

    public String getMotifCorrection() { return motifCorrection; }
    public void setMotifCorrection(String motifCorrection) { this.motifCorrection = motifCorrection; }

    public Utilisateur getSaisiPar() { return saisiPar; }
    public void setSaisiPar(Utilisateur saisiPar) { this.saisiPar = saisiPar; }

    public Utilisateur getScellePar() { return scellePar; }
    public void setScellePar(Utilisateur scellePar) { this.scellePar = scellePar; }

    public Instant getScelleLe() { return scelleLe; }
    public void setScelleLe(Instant scelleLe) { this.scelleLe = scelleLe; }

    public String getEmpreinteScellement() { return empreinteScellement; }
    public void setEmpreinteScellement(String empreinteScellement) { this.empreinteScellement = empreinteScellement; }

    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }

    public Instant getModifieLe() { return modifieLe; }
    public void setModifieLe(Instant modifieLe) { this.modifieLe = modifieLe; }
}
