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
import java.lang.InterruptedException;

import java.util.Map;
import java.util.Arrays;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.lang.ProcessBuilder;
import java.lang.Process;

import java.io.FileNotFoundException;
import java.lang.InterruptedException;

import java.util.Arrays;

import hudson.tasks.Maven;
import hudson.tools.ToolProperty;
import hudson.tasks.Maven.MavenInstallation;
import hudson.tasks.Maven.MavenInstaller;

import hudson.DescriptorExtensionList;
import hudson.tools.ToolProperty;
import hudson.tools.ToolPropertyDescriptor;

import hudson.model.Node;
import hudson.FilePath;

import org.apache.commons.io.FileUtils;

import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;

import java.util.List;

/* Post build class for post build action*/
public class RepairnatorPostBuild extends Recorder {

    private String gitUrl;
    private String gitOAuthToken;
    private String gitBranch;
    private String notifyTo;
    private boolean useEmailNotification;
    private boolean useNPEFix;
    private boolean useAstorJKali;
    private boolean useAstorJMut;
    private boolean useNPEFixSafe;
    private boolean useNopolTestExclusionStrategy;
    
    @DataBoundConstructor
    public RepairnatorPostBuild(String gitUrl,String gitOAuthToken,String gitBranch,String notifyTo) {
        this.gitUrl = gitUrl;
        this.gitOAuthToken = gitOAuthToken;
        this.gitBranch = gitBranch;
        this.notifyTo = notifyTo;
        this.useEmailNotification = useEmailNotification;
    }

    public RepairnatorPostBuild() {}

    @DataBoundSetter
    public void setUseNPEFix(boolean useNPEFix) {
        this.useNPEFix = useNPEFix;
    }

    @DataBoundSetter
    public void setUseAstorJKali(boolean useAstorJKali) {
        this.useAstorJKali = useAstorJKali;
    }

    @DataBoundSetter
    public void setUseAstorJMut(boolean useAstorJMut) {
        this.useAstorJMut = useAstorJMut;
    }

    @DataBoundSetter
    public void setUseNPEFixSafe(boolean useNPEFixSafe) {
        this.useNPEFixSafe = useNPEFixSafe;
    }

    @DataBoundSetter
    public void setUseNopolTestExclusionStrategy(boolean useNopolTestExclusionStrategy) {
        this.useNopolTestExclusionStrategy = useNopolTestExclusionStrategy;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public void setGitOAuthToken(String gitOAuthToken) {
        this.gitOAuthToken = gitOAuthToken;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public void setNotifyTo(String notifyTo) {
        this.notifyTo = notifyTo;
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

    public String getNotifyTo() {
        return this.notifyTo;
    }

    public boolean useTLS() {
        return Config.getInstance().useTLSOrSSL();
    }

    public boolean getUseEmailNofication() {
        return this.useEmailNotification;
    }

    public String[] getTools(){
        String dummy = "";
        if (this.useNPEFix) {
            dummy += ",NPEFix";
        }

        if (this.useAstorJKali) {
            dummy += ",AstorJKali";
        }

        if (this.useAstorJMut) {
            dummy += ",AstorJMut";
        }

        if (this.useNPEFixSafe) {
            dummy += ",NPEFixSafe";
        }

        if (this.useNopolTestExclusionStrategy) {
            dummy += ",NopolTestExclusionStrategy";
        }

        return dummy.substring(1,dummy.length()).split(",");
    }

    public void printProcessOutPut(Process process) throws IOException{
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> System.out.println(line));
        }
    }

    public void printAllEnv(AbstractBuild build,BuildListener listener) throws IOException,InterruptedException{
        System.out.println("-----------Printing Env----------");
        final EnvVars env = build.getEnvironment(listener);
        for(String key : env.keySet()) {
            System.out.println(key + ":" + env.get(key));
        }
        System.out.println("---------------------------------");
    }

    public void runRepairnator(EnvVars env) throws IOException,InterruptedException{
        Config config = Config.getInstance();
        System.out.println("jar location " + config.getJarLocation());
        RepairnatorProcessBuilder repProcBuilder = new RepairnatorProcessBuilder()
                                        .useJavaExec(config.getJavaExec())
                                        .atJarLocation(config.getJarLocation())
                                        .onGitUrl(config.getGitUrl())
                                        .onGitBranch(config.getGitBranch())
                                        .onGitOAuth(config.getGitOAuth())
                                        .withSmtpUsername(config.getSmtpUsername())
                                        .withSmtpPassword(config.getSmtpPassword())
                                        .withSmtpServer(config.getSmtpServer())
                                        .withSmtpPort(config.getSmtpPort())
                                        .shouldNotifyTo(config.getNotifyTo())
                                        .withRepairTools(config.getTools())
                                        .asNoTravisRepair()
                                        .alsoCreatePR();

        ProcessBuilder builder = repProcBuilder.build();
        builder.redirectErrorStream(true);
        builder.inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE);
        Process process = builder.start();
        this.printProcessOutPut(process);
        process.waitFor();
    }


    public String decideGitBranch(EnvVars env) {
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

        return branch;
    }

    public String decideGitUrl(EnvVars env) {
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
        return url;
    }


    public boolean authorIsRepairnator(EnvVars env) {
        String author = env.get("ghprbActualCommitAuthor") == null ? "" : env.get("ghprbActualCommitAuthor");
        if (author.equals("repairnator")) {
            System.out.println("The committer is repairnator, no repair will be made");
            return true;
        }
        return false;
    }

    public boolean isCheckPassed(String branch, String url,EnvVars env) {
        /* Error check */

        if (authorIsRepairnator(env)) {
            return false;
        }

        if (branch.equals("")) {
            System.out.println("ERROR: THE PROVIDED GITBRANCH IS EMPTY");
            return false;
        }

        if (url.equals("")) {
            System.out.println("ERROR: THE PROVIDED GITBRANCH IS EMPTY");
            return false;
        }

        if (!(this.useNPEFix || this.useAstorJKali || this.useAstorJMut || this.useNPEFixSafe || this.useNopolTestExclusionStrategy)) {
            System.out.println("ERROR: NO TOOL SPECIFIED , NO NEED TO REPAIR");
            return false;
        }

        return true;
    }

    public boolean shouldInstallMaven(EnvVars env) {
        String m2Home = env.get("M2_HOME");
        if (m2Home != null) {
            Config.getInstance().setMavenHome(m2Home);
            return false;
        } 
        return true;
    }

    public void configure(String url,String branch, EnvVars env) {
        Config config = Config.getInstance();

        String javaHome = env.get("JAVA_HOME");
        String javaExec = javaHome + File.separator + "bin" + File.separator + "java";
        String jarLocation =  Config.getInstance().getTempDir().getAbsolutePath() + File.separator +"repairnator.jar";

        config.setJavaExec(javaExec);
        config.setJarLocation(jarLocation);
        config.setGitUrl(url);
        config.setGitBranch(branch);
        config.setGitOAuth(this.gitOAuthToken);
        config.setTools(this.getTools());
        config.setNotifyTo(this.notifyTo);
    }

    public void cleanUp(){
        try {
            FileUtils.cleanDirectory(Config.getInstance().getTempDir());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createGlobalEnvironmentVariables(String key, String value){

        Jenkins instance = Jenkins.getInstance();

        DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = instance.getGlobalNodeProperties();
        List<EnvironmentVariablesNodeProperty> envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class);

        EnvironmentVariablesNodeProperty newEnvVarsNodeProperty = null;
        EnvVars envVars = null;

        if ( envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0 ) {
            newEnvVarsNodeProperty = new hudson.slaves.EnvironmentVariablesNodeProperty();
            globalNodeProperties.add(newEnvVarsNodeProperty);
            envVars = newEnvVarsNodeProperty.getEnvVars();
        } else {
            envVars = envVarsNodePropertyList.get(0).getEnvVars();
        }
        envVars.put(key, value);
        try {
            instance.save();
        } catch(Exception e) {
            System.out.println("Failed to create env variable");
        }
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        System.setOut(listener.getLogger());
        System.setErr(listener.getLogger());
        try {
            EnvVars env = build.getEnvironment(listener);
            String branch = this.decideGitBranch(env);
            String url = this.decideGitUrl(env);

            if (!this.isCheckPassed(branch,url,env)){
                return false;
            }

            this.configure(url,branch,env);

            System.out.println("The following tools will be used : " + Arrays.toString(Config.getInstance().getTools()));
            System.out.println("workspace for repairnator: " + Config.getInstance().getTempDir().getAbsolutePath());

            String snapShotUrl = "https://repo.jenkins-ci.org/snapshots/fr/inria/repairnator/repairnator-pipeline";
            RepairnatorJarDownloader repJarDownloader = new RepairnatorJarDownloader();
            repJarDownloader.downloadJar(snapShotUrl);

            if (this.shouldInstallMaven(env)) {
                System.out.println("M2_HOME is null, proceed installing default maven version 3.6.3");
                MavenCustomInstaller mvn = new MavenCustomInstaller(build,listener);
                mvn.install();
            }

            this.runRepairnator(env);
            this.cleanUp();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

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
        private boolean useEmailNotification;
        private boolean useNPEFix;
        private boolean useAstorJMut;
        private boolean useAstorJKali;
        private boolean useNPEFixSafe;
        private boolean useNopolTestExclusionStrategy;
        private String notifyTo;

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

         public FormValidation doCheckOptions(@QueryParameter boolean useNPEFix, @QueryParameter boolean useAstorJKali, @QueryParameter boolean useAstorJMut,@QueryParameter boolean useNPEFixSafe, @QueryParameter boolean useNopolTestExclusionStrategy) {
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
            req.bindJSON(this, formData);
            useNPEFix = formData.getBoolean("useNPEFix");
            useAstorJKali = formData.getBoolean("useAstorJKali");
            useAstorJMut = formData.getBoolean("useAstorJMut");
            useNPEFixSafe = formData.getBoolean("useNPEFixSafe");
            useNopolTestExclusionStrategy = formData.getBoolean("useNopolTestExclusionStrategy");
            notifyTo = formData.getString("notifyTo");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseNPEFix)
            save();
            return true;
        }

        public boolean useNPEFix() {
            return useNPEFix;
        }

        public boolean useAstorJKali() {
            return useAstorJKali;
        }

        public boolean useAstorJMut() {
            return useAstorJMut;
        }

        public boolean useNPEFixSafe() {
            return useNPEFixSafe;
        }

        public boolean useNopolTestExclusionStrategy() {
            return useNopolTestExclusionStrategy;
        }

        public String getNotifyTo() {
            return notifyTo;
        }
    }
}

