package cm.cenc.sigeb.referentiel.service;

import cm.cenc.sigeb.referentiel.dto.*;
import cm.cenc.sigeb.referentiel.entity.Diocese;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import cm.cenc.sigeb.referentiel.entity.ProvinceEcclesiastique;
import cm.cenc.sigeb.referentiel.repository.DioceseRepository;
import cm.cenc.sigeb.referentiel.repository.ParoisseRepository;
import cm.cenc.sigeb.referentiel.repository.ProvinceRepository;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReferentielService {

    private final ProvinceRepository provinceRepository;
    private final DioceseRepository dioceseRepository;
    private final ParoisseRepository paroisseRepository;

    public ReferentielService(
            ProvinceRepository provinceRepository,
            DioceseRepository dioceseRepository,
            ParoisseRepository paroisseRepository
    ) {
        this.provinceRepository = provinceRepository;
        this.dioceseRepository = dioceseRepository;
        this.paroisseRepository = paroisseRepository;
    }

    @Transactional(readOnly = true)
    public List<ProvinceResponse> listerProvinces() {
        return provinceRepository.findAll().stream()
                .map(p -> new ProvinceResponse(p.getId(), p.getCode(), p.getNomFr(), p.getNomEn()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DioceseResponse> listerDioceses(UUID provinceId) {
        List<Diocese> list = provinceId != null
                ? dioceseRepository.findByProvinceId(provinceId)
                : dioceseRepository.findAll();
        return list.stream().map(this::toDioceseResponse).toList();
    }

    @Transactional
    public DioceseResponse creerDiocese(DioceseCreateRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.ADMIN_NATIONAL) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul l'Administrateur National peut créer un diocèse");
        }

        ProvinceEcclesiastique province = provinceRepository.findById(req.provinceId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PROVINCE_INTROUVABLE", "Province introuvable"));

        String code = req.code().trim().toUpperCase();
        if (dioceseRepository.findByCode(code).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CODE_DIOCESE_EXISTANT", "Ce code de diocèse existe déjà");
        }

        Diocese d = new Diocese();
        d.setProvince(province);
        d.setCode(code);
        d.setTypeDiocese(req.typeDiocese());
        d.setNomFr(req.nomFr().trim());
        d.setNomEn(req.nomEn().trim());
        d.setSiege(req.siege().trim());
        d.setActif(true);

        return toDioceseResponse(dioceseRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<ParoisseResponse> listerParoisses(UUID dioceseId) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        UUID targetDioceseId = dioceseId;
        if (caller.role() == Role.CHANCELIER || caller.role() == Role.CURE || caller.role() == Role.SECRETAIRE) {
            if (targetDioceseId == null) {
                targetDioceseId = caller.dioceseId();
            }
        }
        List<Paroisse> list = targetDioceseId != null
                ? paroisseRepository.findByDioceseId(targetDioceseId)
                : paroisseRepository.findAll();
        return list.stream().map(this::toParoisseResponse).toList();
    }

    @Transactional
    public ParoisseResponse creerParoisse(ParoisseCreateRequest req) {
        UserPrincipal caller = TenantContext.requireCurrentUser();
        if (caller.role() != Role.ADMIN_NATIONAL && caller.role() != Role.CHANCELIER) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                    "Seul l'Administrateur National ou le Chancelier peut créer une paroisse");
        }
        if (caller.role() == Role.CHANCELIER && !req.dioceseId().equals(caller.dioceseId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HORS_PERIMETRE",
                    "Un Chancelier ne peut créer une paroisse que dans son propre diocèse");
        }

        Diocese diocese = dioceseRepository.findById(req.dioceseId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "DIOCESE_INTROUVABLE", "Diocèse introuvable"));

        String code = req.code().trim().toUpperCase();
        if (paroisseRepository.findByDioceseIdAndCode(diocese.getId(), code).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "CODE_PAROISSE_EXISTANT",
                    "Ce code de paroisse existe déjà dans ce diocèse");
        }

        Paroisse p = new Paroisse();
        p.setDiocese(diocese);
        p.setCode(code);
        p.setTypeParoisse(req.typeParoisse());
        p.setNomFr(req.nomFr().trim());
        p.setNomEn(req.nomEn().trim());
        p.setLocalite(req.localite().trim());
        p.setEnteteOfficiel(req.enteteOfficiel());
        p.setActif(true);

        return toParoisseResponse(paroisseRepository.save(p));
    }

    private DioceseResponse toDioceseResponse(Diocese d) {
        return new DioceseResponse(
                d.getId(), d.getProvince().getId(), d.getProvince().getCode(),
                d.getCode(), d.getTypeDiocese(), d.getNomFr(), d.getNomEn(), d.getSiege(), d.isActif()
        );
    }

    private ParoisseResponse toParoisseResponse(Paroisse p) {
        return new ParoisseResponse(
                p.getId(), p.getDiocese().getId(), p.getDiocese().getCode(),
                p.getCode(), p.getTypeParoisse(), p.getNomFr(), p.getNomEn(),
                p.getLocalite(), p.getEnteteOfficiel(), p.isActif()
        );
    }
}
