package cm.cenc.sigeb.referentiel;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "diocese")
public class Diocese {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private ProvinceEcclesiastique province;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "type_diocese", nullable = false, length = 20)
    private String typeDiocese; // DIOCESE ou ARCHIDIOCESE

    @Column(name = "nom_fr", nullable = false, length = 150)
    private String nomFr;

    @Column(name = "nom_en", nullable = false, length = 150)
    private String nomEn;

    @Column(nullable = false, length = 100)
    private String siege;

    @Column(nullable = false)
    private boolean actif = true;

    public Diocese() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ProvinceEcclesiastique getProvince() { return province; }
    public void setProvince(ProvinceEcclesiastique province) { this.province = province; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTypeDiocese() { return typeDiocese; }
    public void setTypeDiocese(String typeDiocese) { this.typeDiocese = typeDiocese; }

    public String getNomFr() { return nomFr; }
    public void setNomFr(String nomFr) { this.nomFr = nomFr; }

    public String getNomEn() { return nomEn; }
    public void setNomEn(String nomEn) { this.nomEn = nomEn; }

    public String getSiege() { return siege; }
    public void setSiege(String siege) { this.siege = siege; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
