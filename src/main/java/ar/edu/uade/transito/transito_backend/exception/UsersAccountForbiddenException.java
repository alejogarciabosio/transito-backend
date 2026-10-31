package ar.edu.uade.transito.transito_backend.exception;

/** La identidad fue reconocida por M1, pero la cuenta no puede autenticarse. */
public class UsersAccountForbiddenException extends RuntimeException {
    public UsersAccountForbiddenException(String message) {
        super(message);
    }
}
