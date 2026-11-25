package cm.cenc.sigeb.mention.repository;

import cm.cenc.sigeb.mention.entity.MentionMarginale;
import cm.cenc.sigeb.mention.enums.StatutMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MentionMarginaleRepository extends JpaRepository<MentionMarginale, UUID> {
    List<MentionMarginale> findByActeBaptemeIdAndStatutOrderByDateEvenementAsc(UUID acteBaptemeId, StatutMention statut);
    long countByActeBaptemeIdAndStatut(UUID acteBaptemeId, StatutMention statut);
    List<MentionMarginale> findByStatutOrderByDeclareLeDesc(StatutMention statut);
    List<MentionMarginale> findByActeBaptemeParoisseDioceseProvinceIdAndStatutOrderByDeclareLeDesc(UUID provinceId, StatutMention statut);
    List<MentionMarginale> findByActeBaptemeParoisseDioceseIdAndStatutOrderByDeclareLeDesc(UUID dioceseId, StatutMention statut);
    List<MentionMarginale> findByActeBaptemeParoisseIdAndStatutOrderByDeclareLeDesc(UUID paroisseOrigineId, StatutMention statut);
    List<MentionMarginale> findByParoisseDeclaranteIdOrderByDeclareLeDesc(UUID paroisseDeclaranteId);
}
