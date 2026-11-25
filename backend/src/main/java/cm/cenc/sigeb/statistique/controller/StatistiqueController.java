package cm.cenc.sigeb.statistique.controller;

import cm.cenc.sigeb.statistique.dto.StatistiqueGlobaleResponse;
import cm.cenc.sigeb.statistique.service.StatistiqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistiques")
@Tag(name = "Statistiques Pastorales", description = "Agrégations automatiques par périmètre (Paroisse / Diocèse / Province / National)")
public class StatistiqueController {

    private final StatistiqueService statistiqueService;

    public StatistiqueController(StatistiqueService statistiqueService) {
        this.statistiqueService = statistiqueService;
    }

    @GetMapping("/tableau-de-bord")
    @Operation(summary = "Indicateurs clés consolidés selon le niveau ecclésial de l'utilisateur")
    public ResponseEntity<StatistiqueGlobaleResponse> tableauDeBord() {
        return ResponseEntity.ok(statistiqueService.tableauDeBord());
    }
}
