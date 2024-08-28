package com.ws.calculator;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.tempuri.AddResponse;
import org.tempuri.Add;

@Endpoint
public class CalculatorEndpoint {
  private static final String NAMESPACE_URI = "http://tempuri.org/";


	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "Add")
	@ResponsePayload
	public AddResponse getCountry(@RequestPayload Add request) {
		AddResponse response = new AddResponse();
		 
    response.setAddResult(request.getIntA() + request.getIntB());
		return response;
	}

}
