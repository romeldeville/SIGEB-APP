package cm.cenc.sigeb.bapteme.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "compteur_iun")
public class CompteurIun {

    @EmbeddedId
    private CompteurIunId id;

    @Column(name = "dernier_numero", nullable = false)
    private Integer dernierNumero = 0;

    public CompteurIun() {}

    public CompteurIun(CompteurIunId id, Integer dernierNumero) {
        this.id = id;
        this.dernierNumero = dernierNumero;
    }

    public CompteurIunId getId() { return id; }
    public void setId(CompteurIunId id) { this.id = id; }

    public Integer getDernierNumero() { return dernierNumero; }
    public void setDernierNumero(Integer dernierNumero) { this.dernierNumero = dernierNumero; }
}
