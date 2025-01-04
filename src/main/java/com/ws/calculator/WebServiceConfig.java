package com.ws.calculator;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapMessageFactory;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import java.util.List;
import java.util.Properties;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {
	
	@Override
  public void addInterceptors(List<EndpointInterceptor> interceptors) {
      interceptors.add(new GlobalEndpointInterceptor());
			}
	
	@Bean("messageFactory")
	public SoapMessageFactory messageFactory() {
   var messageFactory = new DualProtocolSaajSoapMessageFactory();

   return messageFactory;
	}

	@Bean
	public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		servlet.setTransformWsdlLocations(true);
		return new ServletRegistrationBean<>(servlet, "/ws/*");
	}

	@Bean(name = "calculator")
	public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema calculatorSchema) {
		DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
		wsdl11Definition.setPortTypeName("CalculatorPort");
		wsdl11Definition.setLocationUri("/ws");
		wsdl11Definition.setTargetNamespace(CalculatorEndpoint.NAMESPACE_URI);
		wsdl11Definition.setSchema(calculatorSchema);
		wsdl11Definition.setCreateSoap12Binding(true); // Enable SOAP 1.2


		// fix for adding soapAction to the dynamic generated wsdl
		Properties soapActions = new Properties();
		soapActions.setProperty("Add", CalculatorEndpoint.NAMESPACE_URI + "Add");
		soapActions.setProperty("Divide", CalculatorEndpoint.NAMESPACE_URI + "Divide");
		soapActions.setProperty("Multiply", CalculatorEndpoint.NAMESPACE_URI + "Multiply");
		soapActions.setProperty("Subtract", CalculatorEndpoint.NAMESPACE_URI + "Subtract");
		wsdl11Definition.setSoapActions(soapActions);

		return wsdl11Definition;
	}

	@Bean
	public XsdSchema calculatorSchema() {
		return new SimpleXsdSchema(new ClassPathResource("calculator.xsd"));
	}
}