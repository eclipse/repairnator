package io.jenkins.plugins.main;
import java.io.File;
import com.google.common.io.Files;
import java.util.HashMap;

public class Config {
	private static Config config;
	private File tempDir;
	private boolean quiet = false;
	private HashMap<String,String> stringConfig;
	private String[] tools;


	private Config(){
		this.stringConfig = new HashMap<String,String>();
	}

	public static Config getInstance() {
		if (config == null) {
			config = new Config();
		}
		return config;
	}

	public File getTempDir() {
		if (this.tempDir == null) {
			try {
				this.tempDir = Files.createTempDir();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return this.tempDir;
	}

	public String getMavenHome() {
		if (!stringConfig.containsKey("mavenHome")) {
			stringConfig.put("mavenHome",this.getTempDir().getAbsolutePath() + File.separator + "maven");
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

	public void setSmtpUsername(String smtpUsername) {
		stringConfig.put("smtpUsername",smtpUsername);
	}

	public String getSmtpUsername() {
		return stringConfig.get("smtpUsername");
	}

	public void setSmtpPassword(String smtpPassword) {
		stringConfig.put("smtpPassword",smtpPassword);
	}

	public String getSmtpPassword() {
		return stringConfig.get("smtpPassword");
	}

	public void setSmtpServer(String smtpServer) {
		stringConfig.put("smtpServer",smtpServer);
	}

	public String getSmtpServer() {
		return stringConfig.get("smtpServer");
	}

	public void setSmtpPort(String smtpPort) {
		stringConfig.put("smtpPort",smtpPort);
	}

	public String getSmtpPort() {
		return stringConfig.get("smtpPort");
	}

	/* format: person1,person2,person3.. */
	public void setNotifyTo(String notifyTo) {
		stringConfig.put("notifyTo",notifyTo);
	}

	public String getNotifyTo() {
		return stringConfig.get("notifyTo");
	}


	public boolean isQuiet() {
		return this.quiet;
	}

	public void switchQuiet() {
		this.quiet = !this.quiet;
	}
}