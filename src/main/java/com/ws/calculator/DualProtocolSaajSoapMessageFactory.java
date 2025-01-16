package com.ws.calculator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.ws.InvalidXmlException;
import org.springframework.ws.soap.SoapMessageCreationException;
import org.springframework.ws.soap.SoapMessageFactory;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.soap.saaj.support.SaajUtils;
import org.springframework.ws.transport.TransportInputStream;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

public class DualProtocolSaajSoapMessageFactory implements SoapMessageFactory, InitializingBean {

  ThreadLocal<MessageFactory> threadLocalValue = new ThreadLocal<>();
  
  private String messageFactoryProtocol;
  private boolean langAttributeOnSoap11FaultString = true;
  private Map<String, ?> messageProperties;
  MessageFactory messageFactory11;
  MessageFactory messageFactory12;
  
  public DualProtocolSaajSoapMessageFactory() {
      super();
  
      try {
          messageFactory11 = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
          messageFactory12 = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      } catch (Exception ex) {
          throw new SoapMessageCreationException("Could not create SAAJ MessageFactory: " + ex.getMessage(), ex);
      }
  }
  
  public void setMessageProperties(Map<String, ?> messageProperties) {
      this.messageProperties = messageProperties;
  }
  
  public void setLangAttributeOnSoap11FaultString(boolean langAttributeOnSoap11FaultString) {
      this.langAttributeOnSoap11FaultString = langAttributeOnSoap11FaultString;
  }
  
  private MessageFactory getMessageFactoryThreadLocal() {
      return threadLocalValue.get();
  }
  
  public void setSoapVersion(SoapVersion version) {
      if (SaajUtils.getSaajVersion() >= 2) {
          if (SoapVersion.SOAP_11 == version) {
              this.messageFactoryProtocol = "SOAP 1.1 Protocol";
          } else {
              if (SoapVersion.SOAP_12 != version) {
                  throw new IllegalArgumentException("Invalid version [" + version + "]. Expected the SOAP_11 or SOAP_12 constant");
              }
  
              this.messageFactoryProtocol = "SOAP 1.2 Protocol";
          }
      } else if (SoapVersion.SOAP_11 != version) {
          throw new IllegalArgumentException("SAAJ 1.1 and 1.2 only support SOAP 1.1");
      }
  
  }
  
  public void afterPropertiesSet() {
  }
  
  public SaajSoapMessage createWebServiceMessage() {
      try {
          MessageFactory messageFactory = getMessageFactoryThreadLocal();
          SOAPMessage saajMessage = messageFactory.createMessage();
          this.postProcess(saajMessage);
          return new SaajSoapMessage(saajMessage, this.langAttributeOnSoap11FaultString, messageFactory);
      } catch (SOAPException var2) {
          throw new SoapMessageCreationException("Could not create empty message: " + var2.getMessage(), var2);
      }
  }
  
  @Override
  public SaajSoapMessage createWebServiceMessage(InputStream inputStream) throws IOException {
    MimeHeaders mimeHeaders = this.parseMimeHeaders(inputStream);
    try {
        inputStream = checkForUtf8ByteOrderMark(inputStream);
        SOAPMessage saajMessage = null;
        // Content-Type = application/soap+xml
        if (mimeHeaders.getHeader(HttpHeaders.CONTENT_TYPE)[0].contains("application/soap+xml")){
            // An 'action' value without double quote or single quote raises an 400 Error
            // Example: Content-Type:'application/soap+xml;charset=utf-8;action=http://tempuri.org/Add'
            //
            // So, add double quote to 'action' (if present) included in the Content-Type and avoid 400 Error
            // Example: Content-Type:'application/soap+xml;charset=utf-8;action="http://tempuri.org/Add"'
            String header = mimeHeaders.getHeader(HttpHeaders.CONTENT_TYPE)[0];
            String [] contents = header.split(";");
            String charset = "";
            String soapAction = null;
            for (String content : contents){
                if (content.contains ("action")){
                    String [] action = content.split("=");
                    if (action != null){
                        if (action.length == 1){
                            soapAction = "\"\"";
                        }
                        else if (action.length == 2){
                            // Add a leading double quote (if there is no double quote or single quote)
                            char lastCharacter = action[1].charAt(0);
                            if (lastCharacter != '"' && lastCharacter != '\'') {
                                soapAction = '"' + action[1];
                                // Add a trailing double quote (if there is no double quote or single quote)
                                lastCharacter = soapAction.charAt(soapAction.length() - 1);
                                if (lastCharacter != '"' && lastCharacter != '\'') {
                                    soapAction = soapAction + '"';
                                }
                            }
                        }
                        soapAction = "action=" + soapAction;
                    }
                }
                else if (content.contains ("charset")) {
                    charset = content + ";";
                }
            }
            if (soapAction != null){
                mimeHeaders.setHeader(HttpHeaders.CONTENT_TYPE, "application/soap+xml;" + charset + soapAction);
            }
            saajMessage = messageFactory12.createMessage(mimeHeaders, inputStream);
            threadLocalValue.set(messageFactory12);
        }
        // Content-Type = text/xml
        else if (mimeHeaders.getHeader(HttpHeaders.CONTENT_TYPE)[0].contains(MimeTypeUtils.TEXT_XML_VALUE)){
            saajMessage = messageFactory11.createMessage(mimeHeaders, inputStream);
            threadLocalValue.set(messageFactory11);
        }
        // Unknown Content-Type
        else{
           throw new SOAPException ("Invalid Content-type");
        }

        saajMessage.getSOAPPart().getEnvelope();
        this.postProcess(saajMessage);
        return new SaajSoapMessage(saajMessage, this.langAttributeOnSoap11FaultString, getMessageFactoryThreadLocal());
    }
    catch (SOAPException exception) {
        throw new InvalidXmlException("Could not parse XML", exception);          
    }
  }
  
  @SuppressWarnings("rawtypes")
private MimeHeaders parseMimeHeaders(InputStream inputStream) throws IOException {
      MimeHeaders mimeHeaders = new MimeHeaders();
      if (inputStream instanceof TransportInputStream) {
          TransportInputStream transportInputStream = (TransportInputStream) inputStream;
          Iterator headerNames = transportInputStream.getHeaderNames();
  
          while (headerNames.hasNext()) {
              String headerName = (String) headerNames.next();
              Iterator headerValues = transportInputStream.getHeaders(headerName);
  
              while (headerValues.hasNext()) {
                  String headerValue = (String) headerValues.next();
                  StringTokenizer tokenizer = new StringTokenizer(headerValue, ",");
  
                  while (tokenizer.hasMoreTokens()) {
                      mimeHeaders.addHeader(headerName, tokenizer.nextToken().trim());
                  }
              }
          }
      }
  
      return mimeHeaders;
  }
  
  private InputStream checkForUtf8ByteOrderMark(InputStream inputStream) throws IOException {
      PushbackInputStream pushbackInputStream = new PushbackInputStream(new BufferedInputStream(inputStream), 3);
      byte[] bytes = new byte[3];
  
      int bytesRead;
      int n;
      for (bytesRead = 0; bytesRead < bytes.length; bytesRead += n) {
          n = pushbackInputStream.read(bytes, bytesRead, bytes.length - bytesRead);
          if (n <= 0) {
              break;
          }
      }
  
      if (bytesRead > 0 && !this.isByteOrderMark(bytes)) {
          pushbackInputStream.unread(bytes, 0, bytesRead);
      }
  
      return pushbackInputStream;
  }
  
  private boolean isByteOrderMark(byte[] bytes) {
      return bytes.length == 3 && bytes[0] == -17 && bytes[1] == -69 && bytes[2] == -65;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
protected void postProcess(SOAPMessage soapMessage) throws SOAPException {
      if (!CollectionUtils.isEmpty(this.messageProperties)) {
          Iterator var2 = this.messageProperties.entrySet().iterator();
  
          while (var2.hasNext()) {
              Map.Entry<String, ?> entry = (Map.Entry) var2.next();
              soapMessage.setProperty((String) entry.getKey(), entry.getValue());
          }
      }
  
      if ("SOAP 1.1 Protocol".equals(this.messageFactoryProtocol)) {
          MimeHeaders headers = soapMessage.getMimeHeaders();
          if (ObjectUtils.isEmpty(headers.getHeader("SOAPAction"))) {
              headers.addHeader("SOAPAction", "\"\"");
          }
      }
  
  }
  
  public String toString() {
      StringBuilder builder = new StringBuilder("SaajSoapMessageFactory[");
      builder.append(SaajUtils.getSaajVersionString());
      if (SaajUtils.getSaajVersion() >= 2) {
          builder.append(',');
          builder.append(this.messageFactoryProtocol);
      }
  
      builder.append(']');
      return builder.toString();
  }}