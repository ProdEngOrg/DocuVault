package ro.unibuc.prodeng.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    private static final String accessDeniedTemplate = "User %s does not have permission to edit document %s";

    public AccessDeniedException(String userId, String documentGroupId) {
        super(String.format(accessDeniedTemplate, userId, documentGroupId));
    }
}
