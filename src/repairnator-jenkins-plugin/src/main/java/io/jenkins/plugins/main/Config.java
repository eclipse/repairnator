package io.jenkins.plugins.main;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import hudson.tasks.Mailer;

public class Config {
	private static Config config;
	private String setupHomePath;
	private boolean quiet = false;
	private HashMap<String,String> stringConfig;
	private String[] tools;
	private String sonarRules;
	private String soraldRepairMode;
	private int soraldMaxFixesPerRule;
	private int segmentSize;
	private File workspaceDir;

	public Config(){
		this.stringConfig = new HashMap<String,String>();
	}

	public static Config getInstance() {
		if (config == null) {
			config = new Config();
		}
		return config;
	}

	public static void resetConfig() {
		config = new Config();
	}

	public void setSetupHomePath(String setupHomePath) {
		this.setupHomePath = setupHomePath;
	}

	public String getSetupHomePath() {
		return this.setupHomePath;
	}

	public String getMavenHome() {
		if (!stringConfig.containsKey("mavenHome")) {
			stringConfig.put("mavenHome",this.getSetupHomePath() + File.separator + "maven");
		}
		return stringConfig.get("mavenHome");
	}

	public void setMavenHome(String mavenHome) {
		stringConfig.put("mavenHome",mavenHome);
	}

	public void setJavaExec(String javaExec) {
		stringConfig.put("javaExec",javaExec);
	}

	public String getJavaExec() {
		return stringConfig.get("javaExec");
	}

	public void setJarLocation(String jarLocation) {
		stringConfig.put("jarLocation",jarLocation);
	}

	public String getJarLocation() {
		return stringConfig.get("jarLocation");
	}

	/* Git related*/
	public void setGitUrl(String gitUrl) {
		stringConfig.put("gitUrl",gitUrl);
	}

	public String getGitUrl() {
		return stringConfig.get("gitUrl");
	}

	public void setGitBranch(String gitBranch) {
		stringConfig.put("gitBranch",gitBranch);
	}

	public String getGitBranch() {
		return stringConfig.get("gitBranch");
	}

	public void setGitOAuth(String gitOAuth) {
		stringConfig.put("gitOAuth",gitOAuth);
	}

	public String getGitOAuth() {
		return stringConfig.get("gitOAuth");
	}

	public void setTools(String[] tools) {
		this.tools = tools;
	}

	public String[] getTools(){
		return this.tools;
	}

	public String getSmtpUsername() {
		if (Mailer.descriptor().getAuthentication() != null) {
			return Mailer.descriptor().getAuthentication().getUsername();
		}
		return null;
	}

	public String getSmtpPassword() {
		if (Mailer.descriptor().getAuthentication() != null) {
			return Mailer.descriptor().getAuthentication().getPassword().getPlainText();
		}
		return null;
	}

	public String getSmtpServer() {
		return Mailer.descriptor().getSmtpHost();
	}


	public String getSmtpPort() {
		return Mailer.descriptor().getSmtpPort();
	}

	/* format: person1,person2,person3.. */
	public void setNotifyTo(String notifyTo) {
		stringConfig.put("notifyTo",notifyTo);
	}

	public String getNotifyTo() {
		return stringConfig.get("notifyTo");
	}

	public boolean useTLSOrSSL() {
		return Mailer.descriptor().getUseSsl();
	}

	public void setSonarRules(String sonarRules) {
		this.sonarRules = sonarRules;
	}

	public String getSonarRules() {
		return this.sonarRules;
	}

	public boolean isQuiet() {
		return this.quiet;
	}

	public void switchQuiet() {
		this.quiet = !this.quiet;
	}

	public void setSoraldRepairMode(String soraldRepairMode) {
		this.soraldRepairMode = soraldRepairMode;
	}

	public String getSoraldRepairMode() {
		return this.soraldRepairMode;
	}

	public void setSoraldMaxFixesPerRule(int soraldMaxFixesPerRule) {
		this.soraldMaxFixesPerRule = soraldMaxFixesPerRule;
	}

	public int getSoraldMaxFixesPerRule() {
		return this.soraldMaxFixesPerRule;
	}

	public void setSegmentSize(int segmentSize) {
		this.segmentSize = segmentSize;
	}

	public int getSegmentSize() {
		return this.segmentSize;
	}

	public void setWorkspaceDir(File workspaceDir) {
		this.workspaceDir = workspaceDir;
	}

	public File getWorkspaceDir() {
		return this.workspaceDir;
	}
}