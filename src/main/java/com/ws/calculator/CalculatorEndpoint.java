package com.ws.calculator;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.tempuri.AddResponse;
import org.tempuri.Add;
import org.tempuri.DivideResponse;
import org.tempuri.Divide;
import org.tempuri.MultiplyResponse;
import org.tempuri.Multiply;
import org.tempuri.SubtractResponse;
import org.tempuri.Subtract;

@Endpoint
public class CalculatorEndpoint {
  private static final String NAMESPACE_URI = "http://tempuri.org/";


	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "Add")
	@ResponsePayload
	public AddResponse addResponse(@RequestPayload Add request) {
		AddResponse response = new AddResponse();
		 
    response.setAddResult(request.getIntA() + request.getIntB());
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "Divide")
	@ResponsePayload
	public DivideResponse divideResponse(@RequestPayload Divide request) {
		DivideResponse response = new DivideResponse();
		 
    response.setDivideResult(request.getIntA() / request.getIntB());
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "Multiply")
	@ResponsePayload
	public MultiplyResponse multiplyResponse(@RequestPayload Multiply request) {
		MultiplyResponse response = new MultiplyResponse();
		 
    response.setMultiplyResult(request.getIntA() * request.getIntB());
		return response;
	}

	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "Subtract")
	@ResponsePayload
	public SubtractResponse subtractResponse(@RequestPayload Subtract request) {
		SubtractResponse response = new SubtractResponse();
		 
    response.setSubtractResult(request.getIntA() - request.getIntB());
		return response;
	}

}
