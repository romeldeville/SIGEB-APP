package cm.cenc.sigeb.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        String nomComplet,
        UUID dioceseId,
        UUID paroisseId,
        boolean mdpAChanger,
        boolean totpActive
) {}
