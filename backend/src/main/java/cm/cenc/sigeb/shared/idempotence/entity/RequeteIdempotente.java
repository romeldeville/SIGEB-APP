package cm.cenc.sigeb.shared.idempotence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "requete_idempotente")
public class RequeteIdempotente {

    @EmbeddedId
    private RequeteIdempotenteId id;

    @Column(name = "empreinte_requete", nullable = false, length = 64)
    private String empreinteRequete;

    @Column(name = "statut_http")
    private Short statutHttp;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reponse_json", columnDefinition = "jsonb")
    private Map<String, Object> reponseJson;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private Instant creeLe = Instant.now();

    public RequeteIdempotente() {}

    public RequeteIdempotenteId getId() { return id; }
    public void setId(RequeteIdempotenteId id) { this.id = id; }

    public String getEmpreinteRequete() { return empreinteRequete; }
    public void setEmpreinteRequete(String empreinteRequete) { this.empreinteRequete = empreinteRequete; }

    public Short getStatutHttp() { return statutHttp; }
    public void setStatutHttp(Short statutHttp) { this.statutHttp = statutHttp; }

    public Map<String, Object> getReponseJson() { return reponseJson; }
    public void setReponseJson(Map<String, Object> reponseJson) { this.reponseJson = reponseJson; }

    public Instant getCreeLe() { return creeLe; }
    public void setCreeLe(Instant creeLe) { this.creeLe = creeLe; }
}
