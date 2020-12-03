package com.fortinet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Run;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;

public class JenkinsUtils {
    @SuppressFBWarnings(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification="Jenkins.getInstance() is not null")
    private static Jenkins getJenkinsInstance() {
        return Jenkins.get(); // getInstance() is deprecated
    }

    public static String getRunUrl(@Nonnull Run<?, ?> run) {
        String rootUrl = getJenkinsInstance().getRootUrl();
        return rootUrl == null ? null : rootUrl + run.getUrl();
    }
    public static String getRunJobName(@Nonnull Run<?, ?> run) {
        String jobName = getJenkinsInstance().getDisplayName();
        return jobName == null ? null : jobName + run.getUrl();
    }
    public static String getRunBuildNumber(@Nonnull Run<?, ?> run) {
        String fullName = getJenkinsInstance().getFullDisplayName();
        return fullName == null ? null : fullName + run.getNumber();
    }
}
