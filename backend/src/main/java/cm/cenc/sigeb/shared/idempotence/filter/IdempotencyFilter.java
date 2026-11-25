package cm.cenc.sigeb.shared.idempotence.filter;

import cm.cenc.sigeb.shared.crypto.service.CryptoService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.idempotence.entity.RequeteIdempotente;
import cm.cenc.sigeb.shared.idempotence.service.IdempotenceService;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotenceService idempotenceService;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(
            IdempotenceService idempotenceService,
            CryptoService cryptoService,
            ObjectMapper objectMapper
    ) {
        this.idempotenceService = idempotenceService;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String keyHeader = request.getHeader("Idempotency-Key");
        Optional<UserPrincipal> userOpt = TenantContext.getCurrentUser();

        if (!"POST".equalsIgnoreCase(request.getMethod()) || keyHeader == null || keyHeader.isBlank() || userOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID key;
        try {
            key = UUID.fromString(keyHeader.trim());
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"title\":\"Idempotency-Key invalide\",\"detail\":\"UUID attendu\"}");
            return;
        }

        ContentCachingRequestWrapper reqWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper resWrapper = new ContentCachingResponseWrapper(response);

        String path = request.getRequestURI();
        String method = request.getMethod();
        UUID userId = userOpt.get().id();

        try {
            // Pour lire le corps avant d'exécuter si besoin, on laisse la requête s'exécuter seulement si absente
            // Note: on vérifie d'abord l'empreinte après lecture ou sur la clé si déjà mémorisée
            filterChain.doFilter(reqWrapper, resWrapper);

            String body = new String(reqWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            String empreinte = cryptoService.hashSha256(method + "|" + path + "|" + body);

            int status = resWrapper.getStatus();
            if (status >= 200 && status < 300) {
                Optional<RequeteIdempotente> existing = idempotenceService.verifier(userId, key, empreinte);
                if (existing.isEmpty()) {
                    byte[] resBytes = resWrapper.getContentAsByteArray();
                    Map<String, Object> json = resBytes.length > 0
                            ? objectMapper.readValue(resBytes, new TypeReference<>() {})
                            : Map.of();
                    idempotenceService.memoriser(userId, key, empreinte, status, json);
                }
            }
            resWrapper.setHeader("Idempotency-Key", key.toString());
            resWrapper.copyBodyToResponse();
        } catch (BusinessException ex) {
            response.setStatus(ex.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"errorCode\":\"" + ex.getCode() + "\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }
}
