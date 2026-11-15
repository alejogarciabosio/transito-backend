package ar.edu.uade.transito.transito_backend.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ar.edu.uade.transito.transito_backend.support.AbstractIntegrationTest;

/** Contrato del WSDL servido en /soap/authentication?wsdl contra el versionado en docs/contratos-integracion. */
@DisplayName("WSDL de la fachada SOAP: XML válido, service/operation esperados e igual al versionado")
class SoapWsdlContractTest extends AbstractIntegrationTest {

    private static final String WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
    private static final String SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";
    private static final Path VERSIONED = Path.of("docs/contratos-integracion/authentication.wsdl");

    private String served() throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/soap/authentication").param("wsdl", ""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Element only(Document doc, String ns, String name) {
        NodeList nodes = doc.getElementsByTagNameNS(ns, name);
        assertThat(nodes.getLength()).as("elementos <%s>", name).isEqualTo(1);
        return (Element) nodes.item(0);
    }

    @Test
    @DisplayName("GET ?wsdl devuelve XML WSDL 1.1 con service AuthenticationService y operación authenticateUser")
    void wsdlHasExpectedServiceAndOperation() throws Exception {
        Document doc = parse(served());
        Element root = doc.getDocumentElement();
        assertThat(root.getNamespaceURI()).isEqualTo(WSDL_NS);
        assertThat(root.getLocalName()).isEqualTo("definitions");
        assertThat(root.getAttribute("targetNamespace")).isEqualTo("https://transito.uade.edu.ar/soap/authentication");

        Element service = only(doc, WSDL_NS, "service");
        assertThat(service.getAttribute("name")).isEqualTo("AuthenticationService");
        Element address = only(doc, SOAP_NS, "address");
        assertThat(address.getAttribute("location")).isEqualTo("/soap/authentication");

        Element portType = only(doc, WSDL_NS, "portType");
        Element operation = (Element) portType.getElementsByTagNameNS(WSDL_NS, "operation").item(0);
        assertThat(operation.getAttribute("name")).isEqualTo("authenticateUser");
        assertThat(only(doc, SOAP_NS, "operation").getAttribute("soapAction")).isEqualTo("authenticateUser");
        assertThat(only(doc, SOAP_NS, "binding").getAttribute("style")).isEqualTo("document");
    }

    @Test
    @DisplayName("el WSDL servido coincide con docs/contratos-integracion/authentication.wsdl")
    void servedEqualsVersioned() throws Exception {
        assertThat(Files.exists(VERSIONED)).as("WSDL versionado").isTrue();
        assertThat(normalize(served())).isEqualTo(normalize(Files.readString(VERSIONED, StandardCharsets.UTF_8)));
    }

    private static String normalize(String xml) {
        return xml.replaceAll(">\\s+<", "><").strip();
    }
}
