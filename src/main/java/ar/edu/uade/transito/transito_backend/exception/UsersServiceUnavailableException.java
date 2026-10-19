package ar.edu.uade.transito.transito_backend.exception;

public class UsersServiceUnavailableException extends RuntimeException {

    public UsersServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
