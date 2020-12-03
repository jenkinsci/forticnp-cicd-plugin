package com.fortinet.forticontainer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanResultDto implements Serializable {
    private static final long serialVersionUID = 4431319032248320895L;
}
