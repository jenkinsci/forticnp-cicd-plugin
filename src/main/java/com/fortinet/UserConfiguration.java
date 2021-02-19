package com.fortinet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fortinet.forticontainer.FortiContainerClient;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ComboBoxModel;
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

    // pre-defined web hosts
    static private final String FORTICWP_HOST = "www.forticwp.com";
    static private final String FORTICWP_EU_HOST = "eu.forticwp.com";
    static private final String FORTICWP_QA1_HOST = "qa1.staging.forticwp.com";
    static private final String HTTP_PROTOCOL = "http://";
    static private final String HTTPS_PROTOCOL = "https://";

    static private List<String> WEB_HOSTS =
        new ArrayList<String>(Arrays.asList(HTTP_PROTOCOL + FORTICWP_HOST,
                                            HTTP_PROTOCOL + FORTICWP_EU_HOST,
                                            HTTP_PROTOCOL + FORTICWP_QA1_HOST));

    public static Boolean isWebHost(String host) {
        // remove protocol from host
        String rawHost = host.replace(HTTPS_PROTOCOL, "");
        rawHost = rawHost.replace(HTTP_PROTOCOL, "");

        for (String staticHost : WEB_HOSTS) {
            if (staticHost.contains(rawHost)) {
                //System.out.println(rawHost + " iswebhost = true");
                return true;
            }
        }
        //System.out.println(rawHost + " iswebhost = false");
        return false;
    }

    public Boolean isWebHost() {
        return isWebHost(hostAddress);
    }

    private Secret credentialToken;
    // host address, could be web / controller host
    private String hostAddress;

    public UserConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public String getCredentialToken() {
        return Secret.toString(credentialToken);
    }

    @DataBoundSetter
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
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

    public FormValidation doCheckHostAddress(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please set host address.");
        }
        return FormValidation.ok();
    }

    public ComboBoxModel doFillHostAddressItems() {
        // global web host address is pre-defined, customer could fill local controller host
        ComboBoxModel combobox = new ComboBoxModel();
        for (String host : WEB_HOSTS) {
            combobox.add(host);
        }
        return combobox;
    }

    @POST
    public FormValidation doTestConnection(
            @QueryParameter("credentialToken") String credentialToken,
            @QueryParameter("hostAddress") String hostAddress) {

        // security - only allow admin to check connection
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        if (StringUtils.isEmpty(credentialToken)) {
            return FormValidation.error("Please set access token");
        }

        try {
            // constructor throws exception if fail to connect to the first available controller
            FortiContainerClient containerClient = new FortiContainerClient(hostAddress,
                                                                            credentialToken,
                                                                            !isWebHost(hostAddress));

            return FormValidation.ok("Successfully connected to " + containerClient.getSessionInfo().getControllerHostUrl());
        } catch (Exception ex) {
            return FormValidation.error("Error occured when trying to connect: " + ex.getMessage());
        }
    }
}
