package com.vault.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"serialNumber"})
@XmlRootElement(name = "GetWarrantyStatusRequest")
public class GetWarrantyStatusRequest {
    @XmlElement(required = true)
    private String serialNumber;

    public GetWarrantyStatusRequest() { }
    public GetWarrantyStatusRequest(String serialNumber) { this.serialNumber = serialNumber; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
}
