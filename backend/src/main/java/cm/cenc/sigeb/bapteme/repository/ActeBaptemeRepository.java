package cm.cenc.sigeb.bapteme.repository;

import cm.cenc.sigeb.bapteme.entity.ActeBapteme;
import cm.cenc.sigeb.bapteme.enums.StatutActe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActeBaptemeRepository extends JpaRepository<ActeBapteme, UUID> {

    Optional<ActeBapteme> findByIun(String iun);

    List<ActeBapteme> findAllByOrderByCreeLeDesc();

    List<ActeBapteme> findByStatutOrderByCreeLeDesc(StatutActe statut);

    List<ActeBapteme> findByParoisseDioceseProvinceIdOrderByCreeLeDesc(UUID provinceId);

    List<ActeBapteme> findByParoisseDioceseProvinceIdAndStatutOrderByCreeLeDesc(UUID provinceId, StatutActe statut);

    List<ActeBapteme> findByParoisseDioceseIdOrderByCreeLeDesc(UUID dioceseId);

    List<ActeBapteme> findByParoisseDioceseIdAndStatutOrderByCreeLeDesc(UUID dioceseId, StatutActe statut);

    List<ActeBapteme> findByParoisseIdOrderByCreeLeDesc(UUID paroisseId);

    List<ActeBapteme> findByParoisseIdAndStatutOrderByCreeLeDesc(UUID paroisseId, StatutActe statut);

    boolean existsByParoisseIdAndRegistreTomeAndRegistrePageAndRegistreNumero(
            UUID paroisseId, String tome, String page, String numero);

    @Query("""
        SELECT a FROM ActeBapteme a
        WHERE LOWER(a.nom) = LOWER(:nom)
          AND LOWER(a.prenoms) = LOWER(:prenoms)
          AND a.dateNaissance = :dateNaissance
        """)
    List<ActeBapteme> findDoublonsPotentiels(String nom, String prenoms, LocalDate dateNaissance);
}
