package com.ws.calculator;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.transport.context.TransportContextHolder;
import org.springframework.ws.transport.http.HttpServletConnection;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPPart;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPException;

import java.net.InetAddress;
import java.util.Iterator;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.soap.Node;

import java.net.UnknownHostException;

public class GlobalEndpointInterceptor implements EndpointInterceptor {
  
  String envRegion   = "X_SOAP_REGION";
  String headerRegion = "X-SOAP-Region";

  @Override
  public boolean handleRequest(MessageContext messageContext, Object o) throws Exception {
    if (messageContext.getRequest() instanceof SoapMessage) {
    SoapMessage soapMessage = (SoapMessage) messageContext.getRequest();
    String soapAction = soapMessage.getSoapAction();
    if (soapAction == null || soapAction == "\"\"") {
        System.out.println("SOAPAction is null");
        throw new Exception ("SOAPAction is null");
    } else {
        System.out.println("SOAPAction: " + soapAction);
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