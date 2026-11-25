package cm.cenc.sigeb.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle("Erreur métier");
        problem.setType(URI.create("urn:sigeb:error:" + ex.getCode().toLowerCase()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Données de formulaire invalides");
        problem.setTitle("Validation");
        problem.setType(URI.create("urn:sigeb:error:validation"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Identifiant ou mot de passe incorrect");
        problem.setTitle("Authentification échouée");
        problem.setType(URI.create("urn:sigeb:error:auth_failed"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Accès refusé pour ce profil");
        problem.setTitle("Permissions insuffisantes");
        problem.setType(URI.create("urn:sigeb:error:forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue");
        problem.setTitle("Erreur serveur");
        problem.setType(URI.create("urn:sigeb:error:internal"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
