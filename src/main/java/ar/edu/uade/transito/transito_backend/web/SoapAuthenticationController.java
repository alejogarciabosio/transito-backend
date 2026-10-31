package ar.edu.uade.transito.transito_backend.web;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ar.edu.uade.transito.transito_backend.auth.AuthenticatedUser;
import ar.edu.uade.transito.transito_backend.auth.IdentityValidator;
import ar.edu.uade.transito.transito_backend.auth.JwtService;
import ar.edu.uade.transito.transito_backend.exception.InvalidCredentialsException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceBadGatewayException;
import ar.edu.uade.transito.transito_backend.exception.UsersAccountForbiddenException;
import ar.edu.uade.transito.transito_backend.exception.UsersServiceUnavailableException;
import lombok.RequiredArgsConstructor;

/**
 * Fachada SOAP 1.1 de M7. La operación authenticateUser se traduce al cliente
 * REST de M1; M1 no debe exponer SOAP ni conocer este contrato.
 */
@RestController
@RequestMapping("/soap/authentication")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "users.soap-adapter.enabled", havingValue = "true", matchIfMissing = true)
public class SoapAuthenticationController {

    private static final MediaType SOAP_XML = MediaType.parseMediaType("text/xml;charset=UTF-8");
    private static final String NAMESPACE = "https://transito.uade.edu.ar/soap/authentication";

    private final IdentityValidator identityValidator;
    private final JwtService jwtService;

    @GetMapping(params = "wsdl", produces = "text/xml")
    public String wsdl() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions name="AuthenticationService" targetNamespace="%s"
                  xmlns:tns="%s" xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"
                  xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://schemas.xmlsoap.org/wsdl/">
                  <documentation>Fachada SOAP de M7. La autenticación se delega al REST de M1.</documentation>
                  <types><xsd:schema targetNamespace="%s">
                    <xsd:element name="authenticateUserRequest"><xsd:complexType><xsd:sequence>
                      <xsd:element name="gmail" type="xsd:string"/><xsd:element name="password" type="xsd:string"/>
                    </xsd:sequence></xsd:complexType></xsd:element>
                    <xsd:element name="authenticateUserResponse"><xsd:complexType><xsd:sequence>
                      <xsd:element name="token" type="xsd:string"/><xsd:element name="role" type="xsd:string"/>
                      <xsd:element name="expiresInSeconds" type="xsd:long"/>
                    </xsd:sequence></xsd:complexType></xsd:element>
                  </xsd:schema></types>
                  <message name="AuthenticateUserInput"><part name="parameters" element="tns:authenticateUserRequest"/></message>
                  <message name="AuthenticateUserOutput"><part name="parameters" element="tns:authenticateUserResponse"/></message>
                  <portType name="AuthenticationPortType"><operation name="authenticateUser">
                    <input message="tns:AuthenticateUserInput"/><output message="tns:AuthenticateUserOutput"/>
                  </operation></portType>
                  <binding name="AuthenticationSoapBinding" type="tns:AuthenticationPortType">
                    <soap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
                    <operation name="authenticateUser"><soap:operation soapAction="authenticateUser"/>
                      <input><soap:body use="literal"/></input><output><soap:body use="literal"/></output>
                    </operation>
                  </binding>
                  <service name="AuthenticationService"><port name="AuthenticationPort" binding="tns:AuthenticationSoapBinding">
                    <soap:address location="/soap/authentication"/>
                  </port></service>
                </definitions>
                """.formatted(NAMESPACE, NAMESPACE, NAMESPACE);
    }

    @PostMapping(consumes = { "text/xml", "application/soap+xml" })
    public ResponseEntity<String> authenticate(@RequestBody String requestXml) {
        try {
            Document document = parseXml(requestXml);
            String gmail = requiredValue(document, "gmail");
            String password = requiredValue(document, "password");
            AuthenticatedUser user = identityValidator.validate(gmail, password);
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok().contentType(SOAP_XML)
                    .body(responseEnvelope(token, user.role().name(), jwtService.getExpirationSeconds()));
        } catch (InvalidCredentialsException ex) {
            return soapFault(401, "Client.Authentication", ex.getMessage());
        } catch (UsersAccountForbiddenException ex) {
            return soapFault(403, "Client.AccountForbidden", ex.getMessage());
        } catch (UsersServiceUnavailableException ex) {
            return soapFault(503, "Server.UsersUnavailable", "El servicio de autenticación no está disponible");
        } catch (UsersServiceBadGatewayException ex) {
            return soapFault(502, "Server.UsersResponse", "Respuesta inválida del servicio de Usuarios");
        } catch (Exception ex) {
            return soapFault(400, "Client.Request", "Solicitud SOAP inválida");
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private String requiredValue(Document document, String name) {
        var values = document.getElementsByTagNameNS("*", name);
        if (values.getLength() == 0 || values.item(0).getTextContent().isBlank()) {
            throw new IllegalArgumentException("Falta " + name);
        }
        return values.item(0).getTextContent().trim();
    }

    private ResponseEntity<String> soapFault(int status, String code, String message) {
        return ResponseEntity.status(status).contentType(SOAP_XML).body("""
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"><soapenv:Body>
                  <soapenv:Fault><faultcode>%s</faultcode><faultstring>%s</faultstring></soapenv:Fault>
                </soapenv:Body></soapenv:Envelope>
                """.formatted(escapeXml(code), escapeXml(message)));
    }

    private String responseEnvelope(String token, String role, long expiresInSeconds) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:auth="%s"><soapenv:Body>
                  <auth:authenticateUserResponse><auth:token>%s</auth:token><auth:role>%s</auth:role>
                  <auth:expiresInSeconds>%d</auth:expiresInSeconds></auth:authenticateUserResponse>
                </soapenv:Body></soapenv:Envelope>
                """.formatted(NAMESPACE, escapeXml(token), escapeXml(role), expiresInSeconds);
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
