package com.vault.soap;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

@Component
public class WarrantySoapClient {
    private final WebServiceTemplate webServiceTemplate;

    public WarrantySoapClient(Jaxb2Marshaller warrantyMarshaller) {
        webServiceTemplate = new WebServiceTemplate(warrantyMarshaller);
        webServiceTemplate.setDefaultUri("http://localhost:8080/ws");
    }

    public GetWarrantyStatusResponse getWarrantyStatus(String serialNumber) {
        return (GetWarrantyStatusResponse) webServiceTemplate.marshalSendAndReceive(
                new GetWarrantyStatusRequest(serialNumber));
    }

    public GetWarrantyStatusResponse getWarrantyStatus(String uri, String serialNumber) {
        return (GetWarrantyStatusResponse) webServiceTemplate.marshalSendAndReceive(
                uri, new GetWarrantyStatusRequest(serialNumber));
    }
}
