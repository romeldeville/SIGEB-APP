package cm.cenc.sigeb.carte.repository;

import cm.cenc.sigeb.carte.entity.CarteEmise;
import cm.cenc.sigeb.carte.enums.FormatDocument;
import cm.cenc.sigeb.carte.enums.StatutCarte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarteEmiseRepository extends JpaRepository<CarteEmise, UUID> {

    Optional<CarteEmise> findByQrToken(String qrToken);

    Optional<CarteEmise> findByActeBaptemeIdAndFormatDocumentAndStatut(
            UUID acteBaptemeId, FormatDocument formatDocument, StatutCarte statut);

    List<CarteEmise> findByActeBaptemeIdOrderByEmisLeDesc(UUID acteBaptemeId);

    @Query("SELECT COALESCE(MAX(c.numeroDuplicata), -1) FROM CarteEmise c WHERE c.acteBapteme.id = :acteId AND c.formatDocument = :format")
    int findMaxNumeroDuplicata(UUID acteId, FormatDocument format);
}
