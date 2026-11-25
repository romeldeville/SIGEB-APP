package cm.cenc.sigeb.auth.dto;

public record TotpSetupResponse(
        String secret,
        String otpAuthUri
) {}
