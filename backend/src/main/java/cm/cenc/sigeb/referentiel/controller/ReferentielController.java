package cm.cenc.sigeb.referentiel.controller;

import cm.cenc.sigeb.referentiel.dto.*;
import cm.cenc.sigeb.referentiel.service.ReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referentiel")
@Tag(name = "Référentiel Ecclésial", description = "Provinces ecclésiastiques, Diocèses et Paroisses du Cameroun")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping("/provinces")
    @Operation(summary = "Liste des 5 provinces ecclésiastiques du Cameroun")
    public ResponseEntity<List<ProvinceResponse>> listerProvinces() {
        return ResponseEntity.ok(referentielService.listerProvinces());
    }

    @GetMapping("/dioceses")
    @Operation(summary = "Liste des diocèses (filtrable par province)")
    public ResponseEntity<List<DioceseResponse>> listerDioceses(@RequestParam(required = false) UUID provinceId) {
        return ResponseEntity.ok(referentielService.listerDioceses(provinceId));
    }

    @PostMapping("/dioceses")
    @Operation(summary = "Créer un diocèse ou archidiocèse (Admin National)")
    public ResponseEntity<DioceseResponse> creerDiocese(@Valid @RequestBody DioceseCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referentielService.creerDiocese(req));
    }

    @GetMapping("/paroisses")
    @Operation(summary = "Liste des paroisses (filtrable par diocèse)")
    public ResponseEntity<List<ParoisseResponse>> listerParoisses(@RequestParam(required = false) UUID dioceseId) {
        return ResponseEntity.ok(referentielService.listerParoisses(dioceseId));
    }

    @PostMapping("/paroisses")
    @Operation(summary = "Créer une paroisse / mission (Admin National ou Chancelier)")
    public ResponseEntity<ParoisseResponse> creerParoisse(@Valid @RequestBody ParoisseCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referentielService.creerParoisse(req));
    }
}
