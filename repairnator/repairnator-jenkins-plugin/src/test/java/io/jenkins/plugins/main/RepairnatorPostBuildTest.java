package io.jenkins.plugins.main;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/* Does not work yet due to dependency collisions with jenkins core*/
public class RepairnatorPostBuildTest {
    public @Rule JenkinsRule jenkins = new JenkinsRule();

    final String gitUrl = "https://github.com/surli/failingProject.git";
    final String gitOAuthToken = "";
    final String gitBranch = "master";

    @Test
    public void testNPEFIX() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        RepairnatorPostBuild postBuild = new RepairnatorPostBuild(gitUrl,gitOAuthToken,gitBranch);
        project.getPublishersList().add(postBuild);
		project = jenkins.configRoundtrip(project);

        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("INFO: PIPELINE FINDING: PATCHED", build);
    }
}