package io.jenkins.plugins.main;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.Action;
import hudson.EnvVars;
import hudson.tasks.*;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundSetter;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import javax.servlet.ServletException;
import java.io.IOException;

import java.io.PrintStream;

import java.util.logging.*;
import java.io.IOException;

import java.util.Map;
import hudson.model.Computer;
import java.util.Arrays;

/* Post build class for post build action*/
public class RepairnatorPostBuild extends Recorder {

    private final String gitUrl;
    private final String gitOAuthToken;
    private final String gitBranch;
    private boolean useNPEFix;
    private boolean useAssertFix;

    // Fields in config.jelly must match the parameter GitUrl in the "DataBoundConstructor"
    @DataBoundConstructor
    public RepairnatorPostBuild(String gitUrl,String gitOAuthToken,String gitBranch) {
        this.gitUrl = gitUrl;
        this.gitOAuthToken = gitOAuthToken;
        this.gitBranch = gitBranch;
    }

    @DataBoundSetter
    public void setUseNPEFix(boolean useNPEFix) {
        this.useNPEFix = useNPEFix;
    }

    @DataBoundSetter
    public void setUseAssertFix(boolean useAssertFix) {
        this.useAssertFix = useAssertFix;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public String getGitOAuthToken() {
        return this.gitOAuthToken;
    }

    public String getGitBranch() {
        return this.gitBranch;
    }


    public String[] getTools(){
        String dummy = "";
        if (this.useNPEFix) {
            dummy += "NPEFix";
        }

        if (this.useAssertFix) {
            dummy += ",AssertFix";
        }

        return dummy.split(",");
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        System.setOut(listener.getLogger());
        System.setErr(listener.getLogger());
        try {
            EnvVars env = build.getEnvironment(listener);
            String author = env.get("ghprbActualCommitAuthor") == null ? "" : env.get("ghprbActualCommitAuthor");
            if (author.equals("repairnator")) {
                System.out.println("The committer is repairnator, no repair will be made");
                return true;
            }
            String branch = "";
            if (this.gitBranch.equals("")) {
                String branchEnv = env.get("GIT_BRANCH");
                branch = branchEnv == null || branchEnv.equals("") ? "master" : env.get("GIT_BRANCH");
                if (branch.split("/").length >= 2) {
                    branch = branch.split("/")[1];
                }
            } else {
                branch = this.gitBranch;
            }

            String url = "";
            if (this.gitUrl.equals("")) {
                String gitUrlEnv = env.get("GIT_URL");
                /* Usual github */
                if (!(env.get("GIT_URL") == null && env.get("GIT_URL").equals(""))) {
                    url = env.get("GIT_URL") + ".git";
                /* Git builder */
                } else if (!(env.get("ghprbAuthorRepoGitUrl") == null && env.get("ghprbAuthorRepoGitUrl").equals(""))) {
                    url = env.get("ghprbAuthorRepoGitUrl");
                }
            } else {
                url = this.gitUrl;
            }

            /* Error check */
            if (branch.equals("")) {
                System.out.println("ERROR: THE PROVIDED GITBRANCH IS EMPTY");
                return false;
            }

            if (url.equals("")) {
                System.out.println("ERROR: THE PROVIDED GITBRANCH IS EMPTY");
                return false;
            }

            if (!(this.useNPEFix || this.useAssertFix)) {
                System.out.println("ERROR: NO TOOL SPECIFIED , NO NEED TO REPAIR");
                return false;
            }

            System.out.println("The following tools will be used : " + Arrays.toString(this.getTools()));
            RunPipelineAction action =  new RunPipelineAction(url,this.gitOAuthToken,branch,this.getTools());
            action.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean testRun() {
        RunPipelineAction action =  new RunPipelineAction(this.gitUrl,this.gitOAuthToken,this.gitBranch,this.getTools());
        action.run();
        return true;
    }
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        return null;
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         * <p/>
         * <p/>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useNPEFix;
        private boolean useAssertFix;
        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         * <p/>
         * Note that returning {@link FormValidation#error(String)} does not
         * prevent the form from being saved. It just means that a message
         * will be displayed to the user.
         */
        public FormValidation doCheckGitUrl(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Example: https://github.com/surli/failingProject.git");
            return FormValidation.ok();
        }

        public FormValidation doCheckGitOAuthToken(@QueryParameter String value )
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Provide a Git Token for Repairnator to make a pull request if patch is found");
            return FormValidation.ok();
        }

        public FormValidation doCheckGitBranch(@QueryParameter String value )
                throws IOException, ServletException {
            return FormValidation.warning("Default should be master or auto detect branch if using together with Jenkins Github plugin");
        }

         public FormValidation doCheckOptions(@QueryParameter boolean useNPEFix,@QueryParameter boolean useAssertFix) {
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Run repairnator";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useNPEFix = formData.getBoolean("useNPEFix");
            useAssertFix = formData.getBoolean("useAssertFix");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseNPEFix)
            save();
            return super.configure(req, formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         * <p/>
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseNPEFix() {
            return useNPEFix;
        }

        public boolean getUseAssertFix() {
            return useAssertFix;
        }
    }
}

