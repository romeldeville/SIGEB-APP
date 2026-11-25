package cm.cenc.sigeb.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role,
        String niveau,
        String nomComplet,
        UUID provinceId,
        UUID dioceseId,
        UUID paroisseId,
        boolean mdpAChanger,
        boolean totpActive
) {}
