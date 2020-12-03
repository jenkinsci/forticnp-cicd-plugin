package com.fortinet.forticontainer;

public class SessionInfo {

    private String controllerHostUrl;
    private String controllerToken;
    private String imageName;

    public SessionInfo(String controllerHostUrl, String controllerToken) {
        this.controllerHostUrl = controllerHostUrl;
        this.controllerToken = controllerToken;
    }


    public String getControllerHostUrl() {
        return controllerHostUrl;
    }

    public void setControllerHostUrl(String controllerHostUrl) {
        this.controllerHostUrl = controllerHostUrl;
    }

    public String getControllerToken() {
        return controllerToken;
    }

    public void setControllerToken(String controllerToken) {
        this.controllerToken = controllerToken;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public String toString() {
        return "controllerHostUrl - : " + controllerHostUrl + " controllerToken - : " + controllerToken;
    }
}
