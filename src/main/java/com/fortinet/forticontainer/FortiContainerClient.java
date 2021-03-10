package com.fortinet.forticontainer;

import com.fortinet.UserConfiguration;
import com.fortinet.forticontainer.common.ControllerUtil;
import com.google.gson.Gson;
import lombok.Data;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

@Data
public class FortiContainerClient {

    private CurrentBuildInfo currentBuildInfo;

    private SessionInfo sessionInfo;

    private Map<String, Boolean> imageResultMap = new HashMap<>();

    private List<String> images = new ArrayList<>();

    public FortiContainerClient(String jobName, String jobUrl, String buildNumber, String imagesNameStr) throws MalformedURLException {
        this.currentBuildInfo = new CurrentBuildInfo(jobName, jobUrl, buildNumber);
        String[] imageList = imagesNameStr.split(",");
        for(String image : imageList) {
            images.add(image);
        }
        currentBuildInfo.setImageNameList(new ArrayList<>(Arrays.asList(imageList)));

        //System.out.println("currentBuildInfo created: " + currentBuildInfo.toString());
    }

    public FortiContainerClient(String hostUrl, String credentialToken, boolean useControllerHost) throws Exception {
        // check physical connection
        String controllerHostUrl = useControllerHost ? hostUrl : ControllerUtil.requestControllerHostUrl(hostUrl, credentialToken);

        // connection is verified in requestControllerHostUrl(), so only check hostUrl
        if (useControllerHost) {
            ControllerUtil.checkControllerConnection(hostUrl, credentialToken);
        }
    
        // session info is aquired only if the physical connection is good
        sessionInfo = new SessionInfo(controllerHostUrl, credentialToken);
    }

    public CurrentBuildInfo imageScan(PrintStream ps) throws Exception {

        UserConfiguration userConfiguration = UserConfiguration.get();
        String controllerHost = ControllerUtil.getControllerHostByUserConfig(userConfiguration, ps);
        if (controllerHost.isEmpty()) {
            throw new RuntimeException("cannot get controller host");
        }

        sessionInfo = new SessionInfo(controllerHost, userConfiguration.getCredentialTokenString());        

        //System.out.println("Protector host address: " + sessionInfo.getControllerHostUrl() + ", token: " + sessionInfo.getControllerToken());
        // try {
            String jobId = JenkinsServer.addJob(sessionInfo, currentBuildInfo);
            if (jobId.isEmpty() || jobId.equals("")) {
                //System.out.println("add job failed, sessionInfo: " + sessionInfo.toString() + ", currentBuildInfo: " + currentBuildInfo.toString());
                throw new RuntimeException("Add job API failed");
            } else {
                Boolean reserve = JenkinsServer.reserveJob(sessionInfo, jobId);
                if (!reserve) {
                    throw new RuntimeException("Cannot reserve image scan job");
                }
            }
            //System.out.println("Add jenkins job to host, the jenkins jobId is " + jobId);
            currentBuildInfo.setJenkinsJobId(jobId);

            // TBD - reserve job id

            final Integer retry = 5;
            final Integer retryDelay = 1000;
            final Integer retryDelayFactor = 2;

            //upload image to controller
            for(String imageName : images) {
                try {
                    Boolean uploadResult = JenkinsServer.uploadImage(jobId, imageName, sessionInfo, ps);
                    imageResultMap.put(imageName, uploadResult);
    
                    if (uploadResult) {
                        ps.println("image: " + imageName + " has been uploaded to host");
                    } else {
                        throw new RuntimeException("image: " + imageName + " was not uploaded to host");
                    }
                } catch (Exception e) {

                } finally {

                }
            }

            ps.println("All the images have been handled, result: "+ new Gson().toJson(imageResultMap));
            //System.out.println("updating jenkins job status to SCANNING");
            boolean status=JenkinsServer.updateJobStatus(sessionInfo, jobId, 10);

            if(status!=true){
                throw new RuntimeException("failed update the job status to SCANNING");
            } else {
                //System.out.println("Successfully updated the job status to SCANNING");
            }
            return currentBuildInfo;
    }

    public Boolean updateJenkinsPolicy(String severityThreshold, String imageName) {
        return false;
    }

    public List<VulnerabilityEntity> getPolicyVulnerability(SessionInfo session) {
        return new ArrayList<>();
    }

}
