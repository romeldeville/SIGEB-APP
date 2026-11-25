package cm.cenc.sigeb.auth;

import cm.cenc.sigeb.auth.dto.*;
import cm.cenc.sigeb.shared.security.TenantContext;
import cm.cenc.sigeb.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Endpoints de connexion, rotation JWT, 2FA et gestion des identifiants")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur (identifiant + mot de passe + code 2FA si activé)")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);
        AuthResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotation du jeton JWT via refresh token rotatif")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changement du mot de passe de l'utilisateur connecté")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-pin")
    @Operation(summary = "Définition ou mise à jour du code PIN de signature (Curé / Chancelier)")
    public ResponseEntity<Void> changePin(@Valid @RequestBody ChangePinRequest request) {
        authService.changePin(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/setup")
    @Operation(summary = "Génération du secret TOTP pour la double authentification")
    public ResponseEntity<TotpSetupResponse> setupTotp() {
        return ResponseEntity.ok(authService.setupTotp());
    }

    @PostMapping("/2fa/confirm")
    @Operation(summary = "Validation du code 2FA pour finaliser l'activation")
    public ResponseEntity<Void> confirmTotp(@Valid @RequestBody TotpVerifyRequest request) {
        authService.confirmTotp(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Récupération du profil de l'utilisateur authentifié")
    public ResponseEntity<UserPrincipal> me() {
        return ResponseEntity.ok(TenantContext.requireCurrentUser());
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
