package com.vault.soap;

import com.vault.config.SoapWebServiceConfig;
import com.vault.entity.Purchase;
import com.vault.service.PurchaseService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class WarrantyStatusEndpoint {
    private final PurchaseService purchaseService;

    public WarrantyStatusEndpoint(PurchaseService purchaseService) { this.purchaseService = purchaseService; }

    @PayloadRoot(namespace = SoapWebServiceConfig.NAMESPACE_URI, localPart = "GetWarrantyStatusRequest")
    @ResponsePayload
    public GetWarrantyStatusResponse getWarrantyStatus(@RequestPayload GetWarrantyStatusRequest request) {
        Purchase purchase = purchaseService.findBySerialNumber(request.getSerialNumber());
        GetWarrantyStatusResponse response = new GetWarrantyStatusResponse();
        response.setProductName(purchase.getProductName());
        response.setBrand(purchase.getBrand() == null ? "" : purchase.getBrand());
        response.setPurchaseDate(purchase.getPurchaseDate().toString());
        response.setWarrantyExpiry(purchase.getWarrantyExpiry().toString());
        response.setDaysLeft(ChronoUnit.DAYS.between(LocalDate.now(), purchase.getWarrantyExpiry()));
        response.setStatus(PurchaseService.status(purchase));
        return response;
    }
}
