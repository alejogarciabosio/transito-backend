package ar.edu.uade.transito.transito_backend.exception;

public class UsersServiceBadGatewayException extends RuntimeException {

    public UsersServiceBadGatewayException(String message) {
        super(message);
    }

    public UsersServiceBadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
