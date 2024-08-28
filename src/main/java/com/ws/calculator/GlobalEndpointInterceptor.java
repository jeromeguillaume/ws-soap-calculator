package com.ws.calculator;

import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPBodyElement;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPPart;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPException;
import java.util.Iterator;
import jakarta.xml.soap.Node;

public class GlobalEndpointInterceptor implements EndpointInterceptor {
  @Override
  public boolean handleRequest(MessageContext messageContext, Object o) throws Exception {
      return true;
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
        SOAPMessage soapMessage = ((SaajSoapMessage) messageContext.getResponse()).getSaajMessage();
        SOAPHeader header = soapMessage.getSOAPHeader();
        SOAPBody body = soapMessage.getSOAPBody();
        // Inject the XML declaration (ie. <?xml version="1.0" encoding="utf-8" ?>)
        soapMessage.setProperty(SOAPMessage.WRITE_XML_DECLARATION, "true");
        SOAPPart soappart = soapMessage.getSOAPPart();
        
        SOAPEnvelope soapenvelope = soappart.getEnvelope ();
        soapenvelope.setPrefix("soap");
        soapenvelope.removeNamespaceDeclaration("SOAP-ENV");
        //soapenvelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
        //soapenvelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        
        // Remove Header
        header.detachNode();
        
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
    } catch (SOAPException soapException) {
        // Handle SOAPException
        throw new Exception("SOAPException occurred", soapException);
    }
  }
}