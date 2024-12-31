# `Calculator` SOAP/XML web service

## Information
The `Calcultator` is a SOAP-based web service server with JAVA Spring and Tomcat.
See [https://spring.io/guides/gs/producing-web-service]( https://spring.io/guides/gs/producing-web-service).
Following endpoints are available:
- `Add`
- `Divide`
- `Multiply`
- `Subtract`

An `X-SOAP-Region` http response header is added. The value is retrieved from the `X_SOAP_REGION` environment variable declared in Tomcat container

The build is done with Maven

## Build and Run with Maven
Build & Run the application with:
```sh
./mvnw spring-boot:run
```
or
---
Build the JAR file with:
```sh
./mvnw clean package
``` 
and then run the JAR file, as follows:
```sh
java -jar target/ws.calculator-1.0.2.jar
```

## Build and Run with Docker
### Build and Push the Docker image for linux/arm64 and linux/amd64
```sh
cd ws-soap-calculator
docker buildx create --use --platform linux/amd64,linux/arm64 --name multi-platform-builder
docker buildx build --push --platform linux/amd64,linux/arm64 --tag jeromeguillaume/ws-soap-calculator:1.0.2 .
```

### Run the Docker image
```sh
docker compose up --build
```
or
---
```sh
docker run -d --name ws-soap-calulator --env X_SOAP_REGION=soap1 -p 8080:8080 jeromeguillaume/ws-soap-calculator:1.0.2
```

## Test
SOAP 1.1 Request (`SOAPAction` is optional):
```sh
http -v POST http://localhost:8080/ws \
Content-Type:"text/xml" \
SOAPAction:"http://tempuri.org/Add" \
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
X-SOAP-Region: soap1
...
<?xml version="1.0" encoding="utf-8" ?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soap:Body>
    <AddResponse xmlns="http://tempuri.org/">
      <AddResult>12</AddResult>
    </AddResponse>
  </soap:Body>
</soap:Envelope>
```

SOAP 1.2 Request (`action` in `Content-Type` is optional):
```sh
http POST http://localhost:8080/ws \
Content-Type:'application/soap+xml;charset=utf-8;action="http://tempuri.org/Add"' \
--raw '<?xml version="1.0" encoding="utf-8"?>
<soap12:Envelope xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
  <soap12:Body>
    <!-- My Comment -->
    <Add xmlns="http://tempuri.org/">
      <intA>5</intA>
      <intB>7</intB>
    </Add>     
  </soap12:Body>
</soap12:Envelope>'
```

Response:
```xml
HTTP/1.1 200
X-SOAP-Region: soap1
...
<?xml version="1.0" encoding="utf-8" ?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:env="http://www.w3.org/2003/05/soap-envelope" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soap:Body>
    <AddResponse xmlns="http://tempuri.org/">
      <AddResult>12</AddResult>
    </AddResponse>
  </soap:Body>
</soap:Envelope>
```
## WSDL
Access to the WSDL: [http://localhost:8080/ws/calculator.wsdl](http://localhost:8080/ws/calculator.wsdl)

## Changelog
- v1.0.0:
  - Initial Release
- v1.0.1:
  - Add `X-SOAP-Region` response HTTP header
- v1.0.2:
  - Add SOAP 1.2 support
  - Check the `SOAPAction` request HTTP header (for SOAP 1.1)
  - Check the `action` request HTTP header, included in `Content-Type` (for SOAP 1.2)