package com.fortinet;

import com.fortinet.forticontainer.FortiContainerClient;
import com.fortinet.forticontainer.JenkinsServer;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

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

    static private final String FORTICWP_HOST = "www.forticwp.com";
    static private final String FORTICWP_EU_HOST = "eu.forticwp.com";
    static private final String DEFAULT_PROTOCOL = "http://";

    private String webHostAddress;
    private Secret credentialToken;
    private String manualHostAddress;
    private boolean enableManualHostCheck;

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

    // return address by check box status
    public String getManualHostAddressByCheck() {
        return enableManualHostCheck ? manualHostAddress : "";
    }

    public String getManualHostAddress() {
        return getManualHostAddressByCheck();
    }
    
    public boolean getEnableManualHostCheck() {
        return enableManualHostCheck;
    }

    @DataBoundSetter
    public void setEnableManualHostCheck(boolean check) {
        this.enableManualHostCheck = check;
        save();
    }

    @DataBoundSetter
    public void setManualHostAddress(String hostAddress) {
        this.manualHostAddress = hostAddress;
        save();
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

    public FormValidation doCheckCredentialToken(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please set access token.");
        }
        return FormValidation.ok();
    }

    // dynamically fill web host list
    public ListBoxModel doFillWebHostAddressItems()
    {
        return new ListBoxModel(new Option("FORTICWP GLOBAL", DEFAULT_PROTOCOL + FORTICWP_HOST),
                                new Option("FORTICWP EU", DEFAULT_PROTOCOL + FORTICWP_EU_HOST),
                                new Option("QA1 (beta release)", DEFAULT_PROTOCOL + "qa1.staging.forticwp.com")); // remove in offical release
    }

    @POST
    public FormValidation doTestConnection(
            @QueryParameter("webHostAddress") String webHostAddress,
            @QueryParameter("credentialToken") String credentialToken,
            @QueryParameter("manualHostAddress") String manualHostAddress,
            @QueryParameter("enableManualHostCheck") boolean enableManualHostCheck) {
        //System.out.println(webHostAddress + "," + manualHostAddress + "," + enableManualHostCheck);

        // only allow admin to check connection
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        if (StringUtils.isEmpty(credentialToken)) {
            return FormValidation.error("Please set access token");
        }

        if (enableManualHostCheck && StringUtils.isEmpty(manualHostAddress)) {
            return FormValidation.error("Please set host address");
        }

        try {
            boolean useControllerHost = enableManualHostCheck && !manualHostAddress.isEmpty();
            String hostAddress = useControllerHost ? manualHostAddress : webHostAddress;

            // constructor throws exception if fail to connect to the first available controller
            FortiContainerClient containerClient = new FortiContainerClient(hostAddress, credentialToken, useControllerHost);
            return FormValidation.ok("Successfully connected to " + containerClient.getSessionInfo().getControllerHostUrl());

        } catch (Exception ex) {
            return FormValidation.error("Error occured: " + ex.getMessage());
        }
    }
}
