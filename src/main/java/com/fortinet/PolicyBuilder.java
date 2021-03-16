package com.fortinet;

import com.fortinet.forticontainer.CurrentBuildInfo;
import com.fortinet.forticontainer.FortiContainerClient;
import com.fortinet.forticontainer.JenkinsServer;
import com.fortinet.forticontainer.SessionInfo;
import com.fortinet.forticontainer.common.ControllerUtil;
import com.google.gson.Gson;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;


public class PolicyBuilder extends Builder implements SimpleBuildStep {

    private String imageName;
    private Boolean block = true;

    @DataBoundConstructor
    public PolicyBuilder(String imageName) {
        this.imageName = imageName;
    }

    @DataBoundSetter
    public void setBlock(Boolean block) {
        if (block == null) {
            this.block = true;
        } else {
            this.block = block;
        }
    }

    public String getImageName() {
        return imageName;
    }

    public Boolean getBlock() {
        return block;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException, RuntimeException {
        PrintStream ps = listener.getLogger();

        VulnerabilityAction existingVulnerabilityAction = run.getAction(VulnerabilityAction.class);
        if (existingVulnerabilityAction != null) {
            ps.println("fortiCWPScanner already did image scanning");
            return;
        }


        UserConfiguration userConfiguration = UserConfiguration.get();
        String controllerHost = ControllerUtil.getControllerHostByUserConfig(userConfiguration, ps);
        if (controllerHost.isEmpty()) {
            run.setResult(Result.FAILURE);
            return;
        }

        SessionInfo sessionInfo = new SessionInfo(controllerHost, userConfiguration.getCredentialTokenString());

        EnvVars envVars = run.getEnvironment(listener);

        String jobName = envVars.get("JOB_NAME");
        String jobUrl = envVars.get("JOB_URL");
        String buildNumber = envVars.get("BUILD_NUMBER");

        //System.out.println("the jobName is " +jobName + ", jobUrl is " + jobUrl + ", buildNumber is " + buildNumber);

        if(imageName == null) {
            ps.println("image name is not set");
            run.setResult(Result.FAILURE);
            return;
        }

        //System.out.println("imageName: " + imageName + ", block: " + block);

        try {
            CurrentBuildInfo currentBuildInfo = new FortiContainerClient(jobName, jobUrl, buildNumber, imageName).imageScan(ps);

            //loop to check scan result
            Integer result = 0;
            final Integer pollingCycle = 10 * 1000;
            while(result <= 0) {
                result = JenkinsServer.checkJobStatus(sessionInfo, currentBuildInfo.getJenkinsJobId(), ps);

                if (result > 0) {
                    ps.println("Image scan result: " + result);
                    break;
                }

                currentBuildInfo.setBuildResult(result);
                Thread.sleep(pollingCycle);
            }

            currentBuildInfo.setBuildResult(result);
            //System.out.println("The jenkins build result for scan vulnerability is " + result + " with build information + " + new Gson().toJson(currentBuildInfo));

            // create the vulnerability action
            VulnerabilityAction vulnerabilityAction = new VulnerabilityAction(currentBuildInfo);
            //System.out.println("Create the action");
            run.addAction(vulnerabilityAction);

            // do publish before fail or success the job based on the scan result
            ps.println("Generating scan result");
            vulnerabilityAction.getImageVulnerabilityReport(ps);
            vulnerabilityAction.getAlertInfo(ps);

            if (block && currentBuildInfo.getBuildResult() == 50) {
                run.setResult(Result.FAILURE);
                throw new RuntimeException("Job is blocked by FortiCWPScanner policy");
            } else {
                run.setResult(Result.SUCCESS);
            }
        } catch (Exception ex) {
            ps.println("Error occured in main task: " + ex.getMessage());
            run.setResult(Result.FAILURE);
        }
    }

    @Symbol("fortiCWPScanner")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckImageName(@QueryParameter String imageName)
                throws IOException, ServletException {
            if (imageName.isEmpty())
                return FormValidation.error("Please enter the image name");
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Scan container images";
        }
    }
}
