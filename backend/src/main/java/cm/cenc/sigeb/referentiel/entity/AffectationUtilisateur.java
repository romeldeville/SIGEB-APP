package cm.cenc.sigeb.referentiel.entity;

import cm.cenc.sigeb.shared.security.model.Role;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "affectation_utilisateur")
public class AffectationUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diocese_id")
    private Diocese diocese;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paroisse_id")
    private Paroisse paroisse;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    public AffectationUtilisateur() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Diocese getDiocese() { return diocese; }
    public void setDiocese(Diocese diocese) { this.diocese = diocese; }

    public Paroisse getParoisse() { return paroisse; }
    public void setParoisse(Paroisse paroisse) { this.paroisse = paroisse; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
}
