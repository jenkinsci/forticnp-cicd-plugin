package com.fortinet.forticontainer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageDto implements Serializable {

    private static final long serialVersionUID = -1047149532457733783L;

    private Long imageId;
    private Long companyId;
    private String imageName;
    private String host;
    private Integer vulnerabilityCount;
    private String osType;
    private String osName;
    private String osVersion;
    private Integer status;
    private Integer result; // 0: failed, 1 success, 2: CANCEL
    private Long jobId;
    private Integer resource;//0 jenkins, 1 job
    private Long scanTime;
    private Long latestCompletedScanTime;
    private String vulnerVersion;
    private String scanningByVulnerVersion;
    private Integer serialNumber;
    private String digest;
    private Boolean latest;
    private Integer riskScore;
    private HashMap<String, Integer> vulnerScoreMap;
    private String project; //for jenkins job display

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getVulnerabilityCount() {
        return vulnerabilityCount;
    }

    public void setVulnerabilityCount(Integer vulnerabilityCount) {
        this.vulnerabilityCount = vulnerabilityCount;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getResource() {
        return resource;
    }

    public void setResource(Integer resource) {
        this.resource = resource;
    }

    public Long getScanTime() {
        return scanTime;
    }

    public void setScanTime(Long scanTime) {
        this.scanTime = scanTime;
    }

    public Long getLatestCompletedScanTime() {
        return latestCompletedScanTime;
    }

    public void setLatestCompletedScanTime(Long latestCompletedScanTime) {
        this.latestCompletedScanTime = latestCompletedScanTime;
    }

    public String getVulnerVersion() {
        return vulnerVersion;
    }

    public void setVulnerVersion(String vulnerVersion) {
        this.vulnerVersion = vulnerVersion;
    }

    public String getScanningByVulnerVersion() {
        return scanningByVulnerVersion;
    }

    public void setScanningByVulnerVersion(String scanningByVulnerVersion) {
        this.scanningByVulnerVersion = scanningByVulnerVersion;
    }

    public Integer getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Integer serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Boolean getLatest() {
        return latest;
    }

    public void setLatest(Boolean latest) {
        this.latest = latest;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public HashMap<String, Integer> getVulnerScoreMap() {
        return vulnerScoreMap;
    }

    public void setVulnerScoreMap(HashMap<String, Integer> vulnerScoreMap) {
        this.vulnerScoreMap = vulnerScoreMap;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
