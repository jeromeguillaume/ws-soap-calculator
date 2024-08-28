# `Calculator` SOAP/XML web service

## Information
The `Calcultator` is a SOAP-based web service server with JAVA Spring and Tomcat.
See [https://spring.io/guides/gs/producing-web-service]( https://spring.io/guides/gs/producing-web-service).
Following endpoints are available:
- `Add`
- `Divide`
- `Multiply`
- `Subtract`

The build is done with Maven

## Build and Run
### Build the SOAP/XML Web Service with Maven
Run the application with:
```sh
./mvnw spring-boot:run
```

Build the JAR file with:
```sh
./mvnw clean package
``` 
and then run the JAR file, as follows:
```sh
java -jar target/ws.calculator-1.0.0.jar
```

### Build a Docker image
- Build and Push the Docker image for linux/arm64 and linux/amd64
```sh
cd ws-soap-calculator
docker buildx create --use --platform linux/amd64,linux/arm64 --name multi-platform-builder
docker buildx build --push --platform linux/amd64,linux/arm64 --tag jeromeguillaume/ws-soap-calculator:1.0.0 .
```

### Run the Docker image
```sh
docker compose up --build
```
or
---
```sh
docker run -d --name ws-soap-calulator -p 8080:8080 jeromeguillaume/ws-soap-calculator:1.0.0
```

## Test
Request:
```sh
http POST http://localhost:8080/ws \
Content-Type:"text/xml; charset=utf-8" \
--raw '<?xml version="1.0" encoding="utf-8"?>
<soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <Add xmlns="http://tempuri.org/">
      <intA>5</intA>
      <intB>7</intB>
    </Add>
  </soap:Body>
</soap:Envelope>'
```

Response:
```xml
HTTP/1.1 200
...
<?xml version="1.0" encoding="utf-8" ?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Body>
    <AddResponse xmlns="http://tempuri.org/">
      <AddResult>12</AddResult>
    </AddResponse>
  </soap:Body>
</soap:Envelope>
```

## WSDL
Access to the WSDL: [http://localhost:8080/ws/calculator.wsdl](http://localhost:8080/ws/calculator.wsdl)