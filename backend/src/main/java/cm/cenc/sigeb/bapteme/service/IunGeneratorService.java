package cm.cenc.sigeb.bapteme.service;

import cm.cenc.sigeb.bapteme.repository.CompteurIunRepository;
import cm.cenc.sigeb.referentiel.entity.Paroisse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IunGeneratorService {

    private final CompteurIunRepository compteurIunRepository;

    public IunGeneratorService(CompteurIunRepository compteurIunRepository) {
        this.compteurIunRepository = compteurIunRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String genererIun(Paroisse paroisse, int anneeBapteme) {
        Integer seq = compteurIunRepository.incrementerEtLire(paroisse.getId(), (short) anneeBapteme);
        String codeDiocese = paroisse.getDiocese().getCode().toUpperCase();
        String codeParoisse = paroisse.getCode().toUpperCase();
        return String.format("CM-%s-%s-%04d-%05d", codeDiocese, codeParoisse, anneeBapteme, seq);
    }
}
