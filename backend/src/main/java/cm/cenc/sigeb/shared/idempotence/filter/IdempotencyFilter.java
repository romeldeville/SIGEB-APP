package cm.cenc.sigeb.shared.idempotence.filter;

import cm.cenc.sigeb.shared.crypto.service.CryptoService;
import cm.cenc.sigeb.shared.exception.BusinessException;
import cm.cenc.sigeb.shared.idempotence.entity.RequeteIdempotente;
import cm.cenc.sigeb.shared.idempotence.service.IdempotenceService;
import cm.cenc.sigeb.shared.security.context.TenantContext;
import cm.cenc.sigeb.shared.security.model.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String ARRAY_WRAPPER_KEY = "_sigeb_raw_payload";

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

        CachedBodyRequestWrapper reqWrapper = new CachedBodyRequestWrapper(request);
        ContentCachingResponseWrapper resWrapper = new ContentCachingResponseWrapper(response);

        String path = request.getRequestURI();
        String method = request.getMethod();
        UUID userId = userOpt.get().id();
        String body = new String(reqWrapper.getCachedBody(), StandardCharsets.UTF_8);
        String empreinte = cryptoService.hashSha256(method + "|" + path + "|" + body);

        try {
            Optional<RequeteIdempotente> existing = idempotenceService.verifier(userId, key, empreinte);
            if (existing.isPresent()) {
                RequeteIdempotente saved = existing.get();
                response.setStatus(saved.getStatutHttp() != null ? saved.getStatutHttp() : HttpServletResponse.SC_OK);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Idempotency-Key", key.toString());
                Map<String, Object> stored = saved.getReponseJson();
                Object payload = (stored != null && stored.size() == 1 && stored.containsKey(ARRAY_WRAPPER_KEY))
                        ? stored.get(ARRAY_WRAPPER_KEY)
                        : stored;
                if (payload != null) {
                    objectMapper.writeValue(response.getWriter(), payload);
                }
                return;
            }

            filterChain.doFilter(reqWrapper, resWrapper);

            int status = resWrapper.getStatus();
            if (status >= 200 && status < 300) {
                byte[] resBytes = resWrapper.getContentAsByteArray();
                Map<String, Object> json = serializeForStorage(resBytes);
                idempotenceService.memoriser(userId, key, empreinte, status, json);
            }
            resWrapper.setHeader("Idempotency-Key", key.toString());
            resWrapper.copyBodyToResponse();
        } catch (BusinessException ex) {
            response.setStatus(ex.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"errorCode\":\"" + ex.getCode() + "\",\"detail\":\"" + ex.getMessage() + "\"}");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> serializeForStorage(byte[] resBytes) throws IOException {
        if (resBytes == null || resBytes.length == 0) {
            return Map.of();
        }
        Object parsed = objectMapper.readValue(resBytes, Object.class);
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of(ARRAY_WRAPPER_KEY, parsed);
    }

    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return bais.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener readListener) {}
                @Override
                public int read() { return bais.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
