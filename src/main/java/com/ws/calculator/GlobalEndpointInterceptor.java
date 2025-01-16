package com.ws.calculator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeTypeUtils;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.soap.Node;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;

public class GlobalEndpointInterceptor implements EndpointInterceptor {
  
  private String    envRegion      = "X_SOAP_REGION";
  private String    headerRegion   = "X-SOAP-Region";
  private Integer   soap11         = 11;
  private Integer   soap12         = 12;

  private void createCustomSoapFault(MessageContext messageContext, String faultDetail) throws SOAPException {
    SoapMessage soapMessage = (SoapMessage) messageContext.getResponse();

    if (soapMessage != null) {
        // Get the SOAPBody from the SoapMessage
        SoapBody soapBody = soapMessage.getSoapBody();

        soapBody.addClientOrSenderFault(faultDetail, Locale.ENGLISH);        
    }
}
  @Override
  public boolean handleRequest(MessageContext messageContext, Object o) throws Exception {
    String soapAction11 = null;
    String soapAction12 = null;
    String soapAction = null;
    String headerContent = null;
    String headerSOAPAction = null;
    Integer soapVersion = 0;
    var transportContext = TransportContextHolder.getTransportContext();
    if (transportContext != null) {
        // Access the HttpServletConnection from the transport context
        HttpServletConnection connection = (HttpServletConnection) transportContext.getConnection();

        if (connection != null) {
            // Retrieve the CONTENT_TYPE HTTP request
            Iterator<String> headersContent = connection.getRequestHeaders(HttpHeaders.CONTENT_TYPE);
            while (headersContent.hasNext())
            {
                headerContent = headersContent.next();
                if (headerContent.contains(MimeTypeUtils.TEXT_XML_VALUE)){
                    soapVersion = soap11;
                }
                else if (headerContent.contains("application/soap+xml")){
                    soapVersion = soap12;
                }
                else {
                    throw new Exception ("Invalid content-type");
                }
            }

            // Retrieve the 'SOAPAction' HTTP request
            Iterator<String> headersSOAPAction = connection.getRequestHeaders("SOAPAction");
            while (headersSOAPAction.hasNext())
            {
                headerSOAPAction = headersSOAPAction.next();
                if (headerSOAPAction != null){
                    soapAction11 = headerSOAPAction;
                    break;
                }
            }
        }
    }
    if (messageContext.getRequest() instanceof SoapMessage) {
        
        if (soapAction11 != null && soapAction11.equals("null")){
            soapAction11 = null;
        }
        else if (soapAction11 == "\"\"") {
            soapAction11 = "";
        }
        
        if (headerContent != null){
            String [] contents = headerContent.split(";");
            for (String content : contents){
                if (content.contains ("action")){
                    // Look for 'action'
                    // Example: 
                    //  action=http://tempuri.org/Add
                    //  action='http://tempuri.org/Add'
                    //  action="http://tempuri.org/Add"
                    String [] action = content.split("=");
                    if (action != null){
                        if (action.length == 1){
                            soapAction12 = "\"\"";
                        }
                        else if (action.length == 2){
                            soapAction12 = action[1];
                            // Remove leading double quote or single quote
                            char firstCharacter = soapAction12.charAt(0);
                            if (firstCharacter == '"' || firstCharacter == '\'') {
                                soapAction12 = soapAction12.substring(1, soapAction12.length());
                            }
                            // Remove leading double quote or single quote
                            char lastCharacter = soapAction12.charAt(soapAction12.length() - 1);
                            if (lastCharacter == '"' || lastCharacter == '\'') {
                                soapAction12 = soapAction12.substring(0, soapAction12.length() - 1);
                            }                            
                        }
                        break;
                    }
                }
            }
        }
    }

    if (soapAction11 != null && soapVersion == soap12){
        throw new Exception ("Found a SOAP 1.2 envelope and a 'SOAPAction' header linked with for SOAP 1.1");
    }
    else if (soapAction12 != null && soapVersion == soap11){
        throw new Exception ("Found a SOAP 1.1 envelope and an 'action' field in the 'Content-Type' header linked with for SOAP 1.2");
    }
    else{
        soapAction = (soapAction11 != null) ? soapAction11 : soapAction12;
    }

    if (soapAction != null){
        SOAPMessage  soapMessage = ((SaajSoapMessage) messageContext.getRequest()).getSaajMessage();
        SOAPBody     body         = soapMessage.getSOAPBody();
        Iterator<Node> it = body.getChildElements();
        String [] actions = soapAction.split("/");
        String actionFromHeader = null;
        if (actions.length > 1){
            actionFromHeader = actions[actions.length-1];
        }
        Boolean soapActionChecked = false;
        while (it.hasNext() && !soapActionChecked) {
            Node node = it.next();
            if ( node.getNodeName().equals(actionFromHeader) ||
                 (node.getNodeName() + "Request").equals(actionFromHeader)  ){
                // WSDL 1.0/WSDL 2.0: example => http://tempuri.org/Subtract
                if ( (CalculatorEndpoint.NAMESPACE_URI + node.getNodeName()).equals(soapAction)){
                    soapActionChecked = true;
                }
                // Default Action Pattern for WSDL 2.0: example => http://tempuri.org/SubtractInterface/SubtractRequest
                else if ( (CalculatorEndpoint.NAMESPACE_URI + node.getNodeName() + "Interface/" + node.getNodeName() + "Request").equals(soapAction)){
                    soapActionChecked = true;
                }
            }
        }
        if (!soapActionChecked){
            createCustomSoapFault(messageContext, "SOAPAction is not valid - This value was sent: soapAction='" + soapAction + "'"); 
            return false;
        }
    }

    return true; // Continue processing
  }

  @Override
  public boolean handleResponse(MessageContext messageContext, Object o) throws Exception {
      return true;
  }

  @Override
  public boolean handleFault(MessageContext messageContext, Object o) throws Exception {
      return false;
  }

  @Override
  public void afterCompletion(MessageContext messageContext, Object o, Exception e) throws Exception {
    try {
        SOAPMessage  soapMessage = ((SaajSoapMessage) messageContext.getResponse()).getSaajMessage();
        SOAPPart     soappart     = soapMessage.getSOAPPart();
        SOAPEnvelope soapenvelope = soappart.getEnvelope();
        SOAPHeader   header       = soapMessage.getSOAPHeader();
        SOAPBody     body         = soapMessage.getSOAPBody();
        
        // Inject the XML declaration (ie. <?xml version="1.0" encoding="utf-8" ?>)
        soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");

        // Remove the SOAP Header
        header.detachNode();
        
        // Replace '<SOAP-ENV:Envelope' by '<soap:Envelope'
        soapenvelope.setPrefix("soap");
        soapenvelope.removeNamespaceDeclaration("SOAP-ENV");
        // Add 'xmlns:xsd=...' and 'xmlns:xsi=...'
        soapenvelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
        soapenvelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        
        // Remove <ns2: and <ns3: namespace prefix
        body.setPrefix("soap");
        Iterator<Node> it = body.getChildElements();
        while (it.hasNext()) {
            Node node = it.next();
            if (node instanceof SOAPBodyElement) {
                SOAPBodyElement bodyElement = (SOAPBodyElement) node;
                
                bodyElement.setPrefix("");
                bodyElement.removeNamespaceDeclaration("ns2");
                Iterator<Node> it2 = bodyElement.getChildElements();
                while (it2.hasNext()) {
                    Node node2 = it2.next();
                    if (node2 instanceof SOAPBodyElement) {
                        SOAPBodyElement bodyElement2 = (SOAPBodyElement) node2;
                        bodyElement2.setPrefix("");
                        Iterator<Node> it3 = bodyElement2.getChildElements();
                        while (it3.hasNext()) {
                            Node node3 = it3.next();
                            if (node3 instanceof SOAPBodyElement) {
                                SOAPBodyElement bodyElement3 = (SOAPBodyElement) node3;
                                bodyElement3.setPrefix("");
                                // Continue processing as needed
                            }
                        }
                    }
                }
            }
        }

        var transportContext = TransportContextHolder.getTransportContext();
        if (transportContext != null) {
            // Access the HttpServletConnection from the transport context
            HttpServletConnection connection = (HttpServletConnection) transportContext.getConnection();

            if (connection != null) {
                // Retrieve the HTTP servlet response
                HttpServletResponse response = connection.getHttpServletResponse();

                // Retrieve the environment variable by name
                String headerRegionValue = System.getenv(envRegion);
                if (headerRegionValue == null){
                    // Get the local host address (the machine the Java program is running on)
                    InetAddress inetAddress = InetAddress.getLocalHost();                    
                    // Retrieve the hostname
                    headerRegionValue = inetAddress.getHostName();                  
                }
                if (headerRegionValue == null){
                  headerRegionValue = "default";
                }
                // Add custom header
                response.addHeader(headerRegion, headerRegionValue);

            }
        }

    } catch (SOAPException soapException) {
        // Handle SOAPException
        throw new Exception("SOAPException occurred", soapException);
    } catch (UnknownHostException hostException) {
      throw new Exception("SOAPException occurred", hostException);
    }
  }
}