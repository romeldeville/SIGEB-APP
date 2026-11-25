package cm.cenc.sigeb.referentiel.repository;

import cm.cenc.sigeb.referentiel.entity.ProvinceEcclesiastique;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProvinceRepository extends JpaRepository<ProvinceEcclesiastique, UUID> {
    Optional<ProvinceEcclesiastique> findByCode(String code);
}
