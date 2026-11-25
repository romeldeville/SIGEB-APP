package cm.cenc.sigeb.referentiel.repository;

import cm.cenc.sigeb.referentiel.entity.Diocese;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DioceseRepository extends JpaRepository<Diocese, UUID> {
    Optional<Diocese> findByCode(String code);
    List<Diocese> findByProvinceId(UUID provinceId);
}
