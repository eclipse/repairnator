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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Date;

import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.BasicFileAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

/* Post build class for post build action*/
public class RepairnatorPostBuild extends Recorder {

    private String gitUrl;
    private String gitOAuthToken;
    private String gitBranch;
    private String notifyTo;
    private boolean useNPEFix;
    private boolean useAstorJKali;
    private boolean useAstorJMut;
    private boolean useNPEFixSafe;
    private boolean useNopolTestExclusionStrategy;
    private boolean useSorald;
    private final Config config = new Config();

    private boolean sonarRulesGiven;
    /* Rules */
    private boolean rule1656;
    private boolean rule1860;
    private boolean rule1948;
    private boolean rule2095;
    private boolean rule2111;
    private boolean rule2116;
    private boolean rule2164;
    private boolean rule2167;
    private boolean rule2184;
    private boolean rule2204;
    private boolean rule2272;
    private boolean rule3032;
    private boolean rule3067;
    private boolean rule3984;
    private boolean rule4973;

    private SonarRulesBlock sonarRulesBlock;

    @DataBoundConstructor
    public RepairnatorPostBuild(String gitUrl,String gitOAuthToken,String gitBranch,String notifyTo,SonarRulesBlock sonarRulesBlock) {
        this.gitUrl = gitUrl;
        this.gitOAuthToken = gitOAuthToken;
        this.gitBranch = gitBranch;
        this.notifyTo = notifyTo;
        this.sonarRulesBlock = sonarRulesBlock;
        if (sonarRulesBlock != null) {
            sonarRulesBlock.rulesProvided = true;
        } 
    }

    public RepairnatorPostBuild() {
    }

    /* Repair Tools*/
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

    @DataBoundSetter
    public void setUseSorald(boolean useSorald) {
        this.useSorald = useSorald;
    }

    public boolean getSonarRulesBlock() {
        return this.sonarRulesBlock != null;
    }

    public boolean getRule1656() {
        return SonarRulesBlock.rule1656;
    }

    public boolean getRule1854() {
        return SonarRulesBlock.rule1854;
    }

    public boolean getRule1860() {
        return SonarRulesBlock.rule1860;
    }

    public boolean getRule1948() {
        return SonarRulesBlock.rule1948;
    }

    public boolean getRule2095() {
        return SonarRulesBlock.rule2095;
    }

    public boolean getRule2111() {
        return SonarRulesBlock.rule2111;
    }

    public boolean getRule2116() {
        return SonarRulesBlock.rule2116;
    }

    public boolean getRule2164() {
        return SonarRulesBlock.rule2164;
    }

    public boolean getRule2167() {
        return SonarRulesBlock.rule2167;
    }

    public boolean getRule2184() {
        return SonarRulesBlock.rule2184;
    }

    public boolean getRule2204() {
        return SonarRulesBlock.rule2204;
    }

    public boolean getRule2272() {
        return SonarRulesBlock.rule2272;
    }

    public boolean getRule3032() {
        return SonarRulesBlock.rule3032;
    }

    public boolean getRule3067() {
        return SonarRulesBlock.rule3067;
    }

    public boolean getRule3984() {
        return SonarRulesBlock.rule3984;
    }

    public boolean getRule4973() {
        return SonarRulesBlock.rule4973;
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
        return this.config.useTLSOrSSL();
    }

    public boolean getUseNPEFix() {
        return useNPEFix;
    }

    public boolean getUseAstorJKali() {
        return useAstorJKali;
    }

    public boolean getUseAstorJMut() {
        return useAstorJMut;
    }

    public boolean getUseNPEFixSafe() {
        return useNPEFixSafe;
    }

    public boolean getUseNopolTestExclusionStrategy() {
        return useNopolTestExclusionStrategy;
    }  

    public boolean getUseSorald() {
        return useSorald;
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

        if (this.useSorald) {
            dummy += ",Sorald";
        }
        return dummy.substring(1,dummy.length()).split(",");
    }



    public Config getConfig() {
        return this.config;
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
        Config config = this.config;
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
                                        .withSonarRules(config.getSonarRules())
                                        .useSmtpTls(config.useTLSOrSSL())
                                        .asNoTravisRepair()
                                        .alsoCreatePR()
                                        .withMavenHome(config.getMavenHome())
                                        .atWorkSpace(config.getTempDir().getAbsolutePath())
                                        .withOutputDir(config.getTempDir().getAbsolutePath());

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

        if (!(this.useNPEFix || this.useAstorJKali || this.useAstorJMut || this.useNPEFixSafe || this.useNopolTestExclusionStrategy || this.useSorald)) {
            System.out.println("ERROR: NO TOOL SPECIFIED , NO NEED TO REPAIR");
            return false;
        }

        return true;
    }

    public boolean shouldInstallMaven(EnvVars env) {
        String m2Home = env.get("M2_HOME");
        File maven = new File(config.getMavenHome());
        if (m2Home != null) {
            this.config.setMavenHome(m2Home);
            return false;
        } 
        if (maven.exists()) {
            return false;
        }
        return true;
    }

    public void configure(String url,String branch, EnvVars env) {
        Config config = this.config;

        String setupHome = env.get("JENKINS_HOME") + File.separator + "userContent" + File.separator + "RepairnatorSetup";
        String javaHome = env.get("JAVA_HOME");
        String javaExec = javaHome + File.separator + "bin" + File.separator + "java";
        String jarLocation =  setupHome + File.separator + "repairnator.jar";

        File setupDir = new File(setupHome);
        if (!setupDir.exists()) {
            setupDir.mkdirs();
        }
        config.setSetupHomePath(setupHome);
        config.setJavaExec(javaExec);
        config.setJarLocation(jarLocation);
        config.setGitUrl(url);
        config.setGitBranch(branch);
        config.setGitOAuth(this.gitOAuthToken);
        config.setTools(this.getTools());
        config.setNotifyTo(this.notifyTo);
        config.setSonarRules(SonarRulesBlock.constructCmdStr4Rules());
    }

    public void cleanUp(){
        try {
            FileUtils.cleanDirectory(this.config.getTempDir());
            this.config.getTempDir().delete();
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

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies,TimeUnit.MILLISECONDS);
    }


    private boolean shouldDownloadJar() throws IOException{
        File jar = new File(this.getConfig().getJarLocation());
        if (jar.exists()) {
            BasicFileAttributes attr = Files.readAttributes(jar.toPath(), BasicFileAttributes.class);
            Date creationTime = Date.from(attr.creationTime().toInstant());
            Date today = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
            if (getDateDiff(creationTime,today,TimeUnit.DAYS) >= 30) { // redownload jar after a month
                System.out.println("Jar will be updated");
                jar.delete();
                return true;
            } else {
                return false;
            }
        } else {
            System.out.println("Jar does not exist, will proceed downloading Jar ...");
            return true;
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

            System.out.println("The following tools will be used : " + Arrays.toString(this.config.getTools()));
            System.out.println("workspace for repairnator: " + this.config.getTempDir().getAbsolutePath());

            String snapShotUrl = "https://repo.jenkins-ci.org/snapshots/fr/inria/repairnator/repairnator-pipeline";

            File jar = new File(this.getConfig().getJarLocation());
            if (this.shouldDownloadJar()) {
                RepairnatorJarDownloader repJarDownloader = new RepairnatorJarDownloader(snapShotUrl,this.getConfig().getJarLocation());
                repJarDownloader.downloadJarHardCoded("https://github.com/henry-lp/mvn-repo/raw/master/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar");
            }

            if (this.shouldInstallMaven(env)) {
                System.out.println("M2_HOME is null, proceed installing default maven version 3.6.3");
                MavenCustomInstaller mvn = new MavenCustomInstaller(build,listener,config.getMavenHome());
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
        private boolean useNPEFix;
        private boolean useAstorJMut;
        private boolean useAstorJKali;
        private boolean useNPEFixSafe;
        private boolean useNopolTestExclusionStrategy;
        private boolean useSorald;
        private boolean rulesProvided;

        public DescriptorImpl() {
            load();
        }

         public FormValidation doCheckOptions(@QueryParameter boolean useNPEFix, @QueryParameter boolean useAstorJKali, @QueryParameter boolean useAstorJMut,@QueryParameter boolean useNPEFixSafe, @QueryParameter boolean useNopolTestExclusionStrategy,@QueryParameter boolean useSorald) {
            if(useSorald) {
                FormValidation.warning("Please also provide sonarRules in the textfield below");
            }
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Run repairnator";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            this.useNPEFix = formData.getBoolean("useNPEFix");
            this.useAstorJKali = formData.getBoolean("useAstorJKali");
            this.useAstorJMut = formData.getBoolean("useAstorJMut");
            this.useNPEFixSafe = formData.getBoolean("useNPEFixSafe");
            this.useNopolTestExclusionStrategy = formData.getBoolean("useNopolTestExclusionStrategy");
            this.useSorald = formData.getBoolean("useSorald");

            save();
            return true;
        }

        public boolean getUseNPEFix() {
            return useNPEFix;
        }

        public boolean getUseAstorJKali() {
            return useAstorJKali;
        }

        public boolean getUseAstorJMut() {
            return useAstorJMut;
        }

        public boolean getUseNPEFixSafe() {
            return useNPEFixSafe;
        }

        public boolean getUseNopolTestExclusionStrategy() {
            return useNopolTestExclusionStrategy;
        }  

        public boolean getUseSorald() {
            return useSorald;
        }
    }

    public static final class SonarRulesBlock {
        public static boolean rulesProvided;

        /* Rules */
        private static boolean rule1656;
        private static boolean rule1854;
        private static boolean rule1860;
        private static boolean rule1948;
        private static boolean rule2095;
        private static boolean rule2111;
        private static boolean rule2116;
        private static boolean rule2164;
        private static boolean rule2167;
        private static boolean rule2184;
        private static boolean rule2204;
        private static boolean rule2272;
        private static boolean rule3032;
        private static boolean rule3067;
        private static boolean rule3984;
        private static boolean rule4973;

        @DataBoundConstructor
        public SonarRulesBlock() {}

        @DataBoundSetter
        public static void setRule1656(boolean rule1656_in) {
            rule1656 = rule1656_in;
        }

        @DataBoundSetter
        public static void setRule1854(boolean rule1854_in) {
            rule1854 = rule1854_in;
        }

        @DataBoundSetter
        public static void setRule1860(boolean rule1860_in) {
            rule1860 = rule1860_in;
        }

        @DataBoundSetter
        public static void setRule1948(boolean rule1948_in) {
            rule1948 = rule1948_in;
        }

        @DataBoundSetter
        public static void setRule2095(boolean rule2095_in) {
            rule2095 = rule2095_in;
        }

        @DataBoundSetter
        public static void setRule2111(boolean rule2111_in) {
            rule2111 = rule2111_in;
        }

        @DataBoundSetter
        public static void setRule2116(boolean rule2116_in) {
            rule2116 = rule2116_in;
        }

        @DataBoundSetter
        public static void setRule2164(boolean rule2164_in) {
            rule2164 = rule2164_in;
        }

        @DataBoundSetter
        public static void setRule2167(boolean rule2167_in) {
            rule2167 = rule2167_in;
        }

        @DataBoundSetter
        public static void setRule2184(boolean rule2184_in) {
            rule2184 = rule2184_in;
        }

        @DataBoundSetter
        public static void setRule2204(boolean rule2204_in) {
            rule2204 = rule2204_in;
        }

        @DataBoundSetter
        public static void setRule2272(boolean rule2272_in) {
            rule2272 = rule2272_in;
        }

        @DataBoundSetter
        public static void setRule3032(boolean rule3032_in) {
            rule3032 = rule3032_in;
        }

        @DataBoundSetter
        public static void setRule3067(boolean rule3067_in) {
            rule3067 = rule3067_in;
        }

        @DataBoundSetter
        public static void setRule3984(boolean rule3984_in) {
            rule3984 = rule3984_in;
        }

        @DataBoundSetter
        public static void setRule4973(boolean rule4973_in) {
            rule4973 = rule4973_in;
        }

        private static String ruleStringOrEmpty(boolean rule,String ruleNumber) {
            if (rule) {
                return ruleNumber + ",";
            }
            return "";
        }

        public static String constructCmdStr4Rules() {
            StringBuilder sb = new StringBuilder();
            sb.append(ruleStringOrEmpty(rule1656,"1656"))
                .append(ruleStringOrEmpty(rule1854,"1854"))
                .append(ruleStringOrEmpty(rule1860,"1860"))
                .append(ruleStringOrEmpty(rule1948,"1948"))
                .append(ruleStringOrEmpty(rule2095,"2095"))
                .append(ruleStringOrEmpty(rule2111,"2111"))
                .append(ruleStringOrEmpty(rule2116,"2116"))
                .append(ruleStringOrEmpty(rule2164,"2164"))
                .append(ruleStringOrEmpty(rule2167,"2167"))
                .append(ruleStringOrEmpty(rule2184,"2184"))
                .append(ruleStringOrEmpty(rule2204,"2204"))
                .append(ruleStringOrEmpty(rule2272,"2272"))
                .append(ruleStringOrEmpty(rule3032,"3032"))
                .append(ruleStringOrEmpty(rule3067,"3067"))
                .append(ruleStringOrEmpty(rule3984,"3984"))
                .append(ruleStringOrEmpty(rule4973,"4973"));

            String res = sb.toString();
            if (!res.equals("")) {
                return res.substring(0,res.length() - 1); // remove last character ','  
            }
            return "";
        }
    }
}

