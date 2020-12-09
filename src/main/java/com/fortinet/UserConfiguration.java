package com.fortinet;

import com.fortinet.forticontainer.FortiContainerClient;
import com.fortinet.forticontainer.JenkinsServer;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

@Extension
public class UserConfiguration extends GlobalConfiguration {

    /** return the singleton instance */
    public static UserConfiguration get() {
        return GlobalConfiguration.all().get(UserConfiguration.class);
    }

    static private final String FORTICWB_HOST = "https://www.forticwp.com";
    static private final String FORTICWB_EU_HOST = "https://eu.forticwp.com";

    // fortics-web host
    private String webHostAddress;
    private Secret credentialToken;
    private String manualControllerHostAddress;

    public UserConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public String getWebHostAddress() {
        return webHostAddress;
    }

    public String getCredentialToken() {
        return Secret.toString(credentialToken);
    }

    public String getManualControllerHostAddress() {
        return manualControllerHostAddress;
    }

    @DataBoundSetter
    public void setWebHostAddress(String webHostAddress) {
        this.webHostAddress = webHostAddress;
        save();
    }

    @DataBoundSetter
    public void setCredentialToken(String credentialToken) {
        this.credentialToken = Secret.fromString(credentialToken);
        save();
    }

    @DataBoundSetter
    public void setManualControllerHostAddress(String controllerHostAddress) {
        this.manualControllerHostAddress = controllerHostAddress;
        save();
    }

    public FormValidation doCheckCredentialToken(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please set access token.");
        }
        return FormValidation.ok();
    }

    // dynamically fill web host list
    public ListBoxModel doFillWebHostAddressItems()
    {
        return new ListBoxModel(new Option("FORTICWB GLOBAL", FORTICWB_HOST),
                                new Option("FORTICWB EU", FORTICWB_EU_HOST));

        // for beta testing, restore if needed
        // new Option("QA (will remove later)", "https://qa.staging.forticwp.com"),
        // new Option("QA1 (will remove later)", "https://qa1.staging.forticwp.com"));
    }

    @POST
    public FormValidation doTestConnection(
            @QueryParameter("webHostAddress") String webHostAddress,
            @QueryParameter("credentialToken") String credentialToken,
            @QueryParameter("manualControllerHostAddress") String manualControllerHostAddress) {

        if (credentialToken.isEmpty()) {
            return FormValidation.error("Please provide access token");
        }

        try {
            boolean useControllerHost = !manualControllerHostAddress.isEmpty();
            String hostAddress = useControllerHost ? manualControllerHostAddress : webHostAddress;

            // constructor throws exception if fail to connect to the first available controller
            FortiContainerClient containerClient = new FortiContainerClient(hostAddress, credentialToken, useControllerHost);
            return FormValidation.ok("Successfully connected to " + containerClient.getSessionInfo().getControllerHostUrl());

        } catch (Exception ex) {
            return FormValidation.error("Error occured: " + ex.getMessage());
        }
    }
}
