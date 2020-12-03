package com.fortinet.forticontainer;

import java.util.List;

public class CurrentBuildInfo {
    private String jobName;
    private String jobUrl;
    private String buildNumber;
//    private String imageName;
    private String jenkinsJobId;
    private Integer buildResult;
    private List<String> imageNameList;

    public CurrentBuildInfo(String jobName, String jobUrl, String buildNumber) {
        this.jobName = jobName;
        this.jobUrl = jobUrl;
        this.buildNumber = buildNumber;

    }

    public List<String> getImageNameList() {
        return imageNameList;
    }

    public void setImageNameList(List<String> imageNameList) {
        this.imageNameList = imageNameList;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public Integer getBuildResult() {
        return buildResult;
    }

    public void setBuildResult(Integer buildResult) {
        this.buildResult = buildResult;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

//    public String getImageName() {
//        return imageName;
//    }

    public String getJenkinsJobId() {
        return jenkinsJobId;
    }

    public void setJenkinsJobId(String jenkinsJobId) {
        this.jenkinsJobId = jenkinsJobId;
    }

    @Override
    public String toString() {
        return "CurrentBuildInfo, name:" + jobName + " url:" + jobUrl + " bulidN number:" + buildNumber + " job ID:" + jenkinsJobId;
    }

    //可以在这边对一些url 做处理，修改成想要的formart
}
