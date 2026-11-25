package cm.cenc.sigeb.referentiel;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "province_ecclesiastique")
public class ProvinceEcclesiastique {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "nom_fr", nullable = false, length = 120)
    private String nomFr;

    @Column(name = "nom_en", nullable = false, length = 120)
    private String nomEn;

    public ProvinceEcclesiastique() {}

    public ProvinceEcclesiastique(String code, String nomFr, String nomEn) {
        this.code = code;
        this.nomFr = nomFr;
        this.nomEn = nomEn;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNomFr() { return nomFr; }
    public void setNomFr(String nomFr) { this.nomFr = nomFr; }

    public String getNomEn() { return nomEn; }
    public void setNomEn(String nomEn) { this.nomEn = nomEn; }
}
