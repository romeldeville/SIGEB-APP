package cm.cenc.sigeb.shared.security.service;

import cm.cenc.sigeb.shared.security.model.NiveauEcclesial;
import cm.cenc.sigeb.shared.security.model.Role;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${sigeb.security.jwt.secret:sigeb-secret-key-cameroon-national-catholic-church-2026-very-secure}") String secret,
            @Value("${sigeb.security.jwt.expiration-minutes:15}") long expirationMinutes) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public String generateAccessToken(UserPrincipal user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        var builder = Jwts.builder()
                .subject(user.identifiant())
                .claim("userId", user.id().toString())
                .claim("role", user.role().name())
                .claim("niveau", user.niveau().name())
                .claim("nomComplet", user.nomComplet())
                .claim("langue", user.langue())
                .claim("mdpAChanger", user.mdpAChanger())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (user.provinceId() != null) {
            builder.claim("provinceId", user.provinceId().toString());
        }
        if (user.dioceseId() != null) {
            builder.claim("dioceseId", user.dioceseId().toString());
        }
        if (user.paroisseId() != null) {
            builder.claim("paroisseId", user.paroisseId().toString());
        }

        return builder.signWith(key).compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserPrincipal extractUserPrincipal(String token) {
        Claims claims = parseAndValidate(token);
        UUID userId = UUID.fromString(claims.get("userId", String.class));
        String identifiant = claims.getSubject();
        Role role = Role.valueOf(claims.get("role", String.class));
        NiveauEcclesial niveau = NiveauEcclesial.valueOf(claims.get("niveau", String.class));
        UUID provinceId = parseUuid(claims.get("provinceId", String.class));
        UUID dioceseId = parseUuid(claims.get("dioceseId", String.class));
        UUID paroisseId = parseUuid(claims.get("paroisseId", String.class));
        String nomComplet = claims.get("nomComplet", String.class);
        String langue = claims.get("langue", String.class);
        Boolean mdpAChanger = claims.get("mdpAChanger", Boolean.class);

        return new UserPrincipal(
                userId, identifiant, "", role, niveau,
                provinceId, dioceseId, paroisseId,
                nomComplet, langue != null ? langue : "FR",
                true, mdpAChanger != null && mdpAChanger, false
        );
    }

    private static UUID parseUuid(String s) {
        return (s == null || s.isBlank()) ? null : UUID.fromString(s);
    }
}
