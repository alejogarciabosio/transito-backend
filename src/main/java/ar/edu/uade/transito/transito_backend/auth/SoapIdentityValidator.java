package ar.edu.uade.transito.transito_backend.auth;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceBadGatewayException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceUnavailableException;
import ar.edu.uade.transito.transito_backend.model.enums.Role;

/** Cliente del contrato SOAP validateUser definido por M1 - Usuarios. */
@Component
@ConditionalOnProperty(name = "users.soap.enabled", havingValue = "true")
public class SoapIdentityValidator implements IdentityValidator {

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String USERS_NS = "http://m1.usuarios/";

    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration timeout;

    public SoapIdentityValidator(@Value("${users.soap.url}") String url,
            @Value("${users.soap.timeout-ms:5000}") long timeoutMs) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("USERS_SOAP_URL es obligatorio cuando USERS_SOAP_ENABLED=true");
        }
        this.timeout = Duration.ofMillis(timeoutMs);
        this.endpoint = URI.create(url);
        this.httpClient = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    @Override
    public AuthenticatedUser validate(String dni, String password) {
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "text/xml; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(requestEnvelope(dni, password)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UsersServiceBadGatewayException("Usuarios respondió HTTP " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (UsersServiceBadGatewayException ex) {
            throw ex;
        } catch (java.net.http.HttpTimeoutException | java.net.ConnectException ex) {
            throw new UsersServiceUnavailableException("El servicio de Usuarios no está disponible", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new UsersServiceUnavailableException("La validación con Usuarios fue interrumpida", ex);
        } catch (Exception ex) {
            throw new UsersServiceBadGatewayException("No fue posible procesar la respuesta de Usuarios", ex);
        }
    }

    private AuthenticatedUser parseResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        if (document.getElementsByTagNameNS(SOAP_NS, "Fault").getLength() > 0) {
            throw new UsersServiceBadGatewayException("Usuarios devolvió un SOAP Fault");
        }
        if (!Boolean.parseBoolean(value(document, "auth"))) {
            throw new InvalidCredentialsException("Usuario o contraseña inválidos");
        }
        return new AuthenticatedUser(value(document, "user_id"), value(document, "dni"),
                Role.valueOf(value(document, "role")));
    }

    private String value(Document document, String name) {
        var nodes = document.getElementsByTagNameNS(USERS_NS, name);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent().isBlank()) {
            throw new UsersServiceBadGatewayException("Respuesta de Usuarios sin campo " + name);
        }
        return nodes.item(0).getTextContent().trim();
    }

    private String requestEnvelope(String dni, String password) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:usr="http://m1.usuarios/">
                  <soapenv:Header/>
                  <soapenv:Body><usr:validateUserRequest><usr:dni>%s</usr:dni><usr:password>%s</usr:password></usr:validateUserRequest></soapenv:Body>
                </soapenv:Envelope>
                """.formatted(escapeXml(dni), escapeXml(password));
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
