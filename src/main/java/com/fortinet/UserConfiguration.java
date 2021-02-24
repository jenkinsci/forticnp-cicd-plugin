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

    public static class OptinalTextBlock {
        private String text;

        @org.kohsuke.stapler.DataBoundConstructor
        public OptinalTextBlock(String text) {
            this.text = text;
        }
    }

    /** return the singleton instance */
    public static UserConfiguration get() {
        return GlobalConfiguration.all().get(UserConfiguration.class);
    }

    static private final String FORTICWP_HOST = "www.forticwp.com";
    static private final String FORTICWP_EU_HOST = "eu.forticwp.com";
    static private final String DEFAULT_PROTOCOL = "http://";

    // fortics-web host
    private String webHostAddress;
    private Secret credentialToken;
    private OptinalTextBlock optionalTextBlock;
    // keep this to be compatible to the existing logic
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
        return getText();
    }

    public String getText() {
        manualControllerHostAddress = (optionalTextBlock == null ? null : optionalTextBlock.text);
        return manualControllerHostAddress;
    }

    // public String getText() {
    //     System.out.println("!!!getText(): " + (optionalTextBlock == null ? "null" : optionalTextBlock.toString()) + ", text: " + optionalTextBlock.text);
    //     System.out.println("it: " + this.getClass());
    //     optionalText = optionalTextBlock == null ? null : optionalTextBlock.manualControllerHostAddress;
    //     return optionalText;
    // }

    @DataBoundSetter
    public void setEnableManualHost(OptinalTextBlock optionalTextBlock) {
        System.out.println("!!!setEnableManualHost(): " + optionalTextBlock.toString() + ", text:" + optionalTextBlock.text);
        System.out.println("it: " + this.getClass());
        this.manualControllerHostAddress = (optionalTextBlock != null) ? optionalTextBlock.text : null;
        this.optionalTextBlock = optionalTextBlock;
        save();
    }

    // @DataBoundSetter
    // public void setOptionalText(OptinalTextBlock enableText) {
    //     System.out.println("!!!setOptionalText(): " + enableText.toString());
    //     this.optionalText = (enableText != null) ? enableText.text : null;
    //     this.optionalTextBlock = enableText;
    //     save();
    // }


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

    // @DataBoundSetter
    // public void setManualControllerHostAddress(String controllerHostAddress) {
    //     this.manualControllerHostAddress = controllerHostAddress;
    //     save();
    // }

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
                                new Option("QA1 (beta release)", DEFAULT_PROTOCOL + "qa1.staging.forticwp.com"));
    }

    @POST
    public FormValidation doTestConnection(
            @QueryParameter("webHostAddress") String webHostAddress,
            @QueryParameter("credentialToken") String credentialToken,
            @QueryParameter("manualControllerHostAddress") String manualControllerHostAddress) {
        // only allow admin to check connection
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        if (StringUtils.isEmpty(credentialToken)) {
            return FormValidation.error("Please set access token");
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
