package io.jenkins.plugins.main;

import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.IllegalArgumentException;
import java.lang.ProcessBuilder;
import java.io.File;

/* Build subprocess to run repairnator Jar */
public class RepairnatorProcessBuilder {
	private final ArrayList<String> cmdList = new ArrayList<String>();
	private static RepairnatorProcessBuilder repairnatorProcessBuilder;
	private String javaExec;
	private String jarLocation;
	private String gitUrl;
	private String gitBranch;
	private String gitOAuth;
	private String smtpUsername;
	private String smtpPassword;
	private String smtpServer;
	private String smtpPort;
	private String notifyTo;
	private String[] repairTools;
	private String workspace;
	private String mavenHome;
	private String outputDir;
	private String sonarRules;
	private boolean createPR;
	private boolean useSmtpTLS;
	private boolean noTravisRepair;

	public RepairnatorProcessBuilder(){}

	public RepairnatorProcessBuilder useJavaExec(String javaExec) {
		this.javaExec = javaExec;
		return this;
	}

	public RepairnatorProcessBuilder atJarLocation(String jarLocation) {
		this.jarLocation = jarLocation;
		return this;
	}

	public RepairnatorProcessBuilder onGitUrl(String gitUrl) {
		this.gitUrl = gitUrl;
		return this;
	}

	public RepairnatorProcessBuilder onGitBranch(String gitBranch) {
		this.gitBranch = gitBranch;
		return this;
	}

	public RepairnatorProcessBuilder onGitOAuth(String gitOAuth) {
		this.gitOAuth = gitOAuth;
		return this;
	}

	public RepairnatorProcessBuilder withRepairTools(String[] repairTools) {
		this.repairTools = repairTools;
		return this;
	}

	public RepairnatorProcessBuilder asNoTravisRepair() {
		this.noTravisRepair = true;
		return this;
	}

	public RepairnatorProcessBuilder alsoCreatePR() {
		/* only createPR if git token is provided*/
		this.createPR = (this.gitOAuth != null || !this.gitOAuth.equals(""));
		return this;
	}

	public RepairnatorProcessBuilder withSmtpUsername(String smtpUsername) {
		this.smtpUsername = smtpUsername;
		return this;
	}

	public RepairnatorProcessBuilder withSmtpPassword(String smtpPassword) {
		this.smtpPassword = smtpPassword;
		return this;
	}

	public RepairnatorProcessBuilder withSmtpServer(String smtpServer) {
		this.smtpServer = smtpServer;
		return this;
	}

	public RepairnatorProcessBuilder withSmtpPort(String smtpPort) {
		this.smtpPort = smtpPort;
		return this;
	}

	public RepairnatorProcessBuilder shouldNotifyTo(String notifyTo) {
		this.notifyTo = notifyTo;
		return this;
	}

	public RepairnatorProcessBuilder withMavenHome(String mavenHome) {
		this.mavenHome = mavenHome;
		return this;
	}

	public RepairnatorProcessBuilder withOutputDir(String outputDir) {
		this.outputDir = outputDir;
		return this;
	}

	public RepairnatorProcessBuilder atWorkSpace(String workspace) {
		this.workspace = workspace;
		return this;
	}

	public RepairnatorProcessBuilder useSmtpTls(boolean smtpTLS) {
		this.useSmtpTLS = smtpTLS;
		return this;
	}

	public RepairnatorProcessBuilder withSonarRules(String sonarRules) {
		this.sonarRules = sonarRules;
		return this;
	}

	public void checkValid() {
		if (this.javaExec == null || this.javaExec.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: java executable location is null");
		}

		if (this.jarLocation == null || this.jarLocation.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: repairnator JAR location is null");
		}

		if (this.gitUrl == null || this.gitUrl.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: no git url provided");
		}

		if (this.gitBranch == null || this.gitBranch.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: no git branch provided");
		}

		if (this.repairTools == null || this.repairTools.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: no repair tools specified");
		}
	}

	public ProcessBuilder build() {
		this.checkValid();

		cmdList.add(this.javaExec);
		cmdList.add("-jar");
		cmdList.add("-Dspoon.log.path=" + this.outputDir + File.separator + "spoon-log.log");
		cmdList.add("-Dlogs_dir=" + this.outputDir);
		cmdList.add(this.jarLocation);
		cmdList.add("--launcherChoice");
		cmdList.add("NEW");
		cmdList.add("--launcherMode");
		cmdList.add("GIT_REPOSITORY");
		cmdList.add("--output");
		cmdList.add(this.outputDir);
		cmdList.add("--MavenHome");
		cmdList.add(this.mavenHome);
		cmdList.add("--workspace");
		cmdList.add(this.workspace);
		cmdList.add("--gitrepourl");
		cmdList.add(this.gitUrl);
		cmdList.add("--gitrepobranch");
		cmdList.add(this.gitBranch);
		cmdList.add("--repairTools");
		cmdList.add(String.join(",",this.repairTools));

		if (!sonarRules.equals("") || sonarRules != null) {
			cmdList.add("--sonarRules");
			cmdList.add(this.sonarRules);
		}

		if (this.notifyTo != null && !this.notifyTo.equals("")) {
			if (this.smtpUsername != null && !this.smtpUsername.equals("")) {
			cmdList.add("--smtpUsername");
			cmdList.add(this.smtpUsername);
			}

			if (this.smtpPassword != null && !this.smtpPassword.equals("")) {
				cmdList.add("--smtpPassword");
				cmdList.add(this.smtpPassword);
			}

			if (this.smtpServer != null && !this.smtpServer.equals("")) {
				cmdList.add("--smtpServer");
				cmdList.add(this.smtpServer);
			}

			if (this.smtpPort != null && !this.smtpPort.equals("")) {
				cmdList.add("--smtpPort");
				cmdList.add(this.smtpPort);
			}

			cmdList.add("--notifyto");
			cmdList.add(String.join(",",this.notifyTo));
		}

		cmdList.add("--gitrepo");
		if (this.useSmtpTLS) {
			cmdList.add("--smtpTLS");
		}
		
		if(!(this.gitOAuth.equals("") || this.gitOAuth == null)) {
			cmdList.add("--ghOauth");
			cmdList.add(this.gitOAuth);
			cmdList.add("--createPR");
		}
		return new ProcessBuilder(this.cmdList).inheritIO();
	}
}