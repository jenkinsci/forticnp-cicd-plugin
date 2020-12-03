package com.fortinet.forticontainer;

import java.io.Serializable;
import java.util.List;

public class QueryResponse implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 3559224754382098715L;
    private int totalSize;
    private List<VulnerabilityEntity> vulnerabilityEntities;

    public QueryResponse(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public List<VulnerabilityEntity> getVulnerabilityEntities() {
        return vulnerabilityEntities;
    }

    public void setVulnerabilityEntities(List<VulnerabilityEntity> vulnerabilityEntities) {
        this.vulnerabilityEntities = vulnerabilityEntities;
    }
}
