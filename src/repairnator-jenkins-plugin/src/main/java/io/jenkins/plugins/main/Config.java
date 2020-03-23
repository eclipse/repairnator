package io.jenkins.plugins.main;
import java.io.File;
import java.nio.file.Files;
/*import com.google.common.io.Files;*/
import java.util.HashMap;
import hudson.tasks.Mailer;

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
				this.tempDir = Files.createTempDirectory(Long.toString(System.nanoTime())).toFile();
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

	public String getSmtpUsername() {
		return Mailer.descriptor().getAuthentication().getUsername();
	}

	public String getSmtpPassword() {
		return Mailer.descriptor().getAuthentication().getPassword().getPlainText();
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

	public boolean useEmailNotification() {
		return this.useEmailNotification;
	}

	public boolean isQuiet() {
		return this.quiet;
	}

	public void switchQuiet() {
		this.quiet = !this.quiet;
	}
}