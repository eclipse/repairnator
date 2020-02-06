package io.jenkins.plugins.main;

import java.lang.StringBuilder;
import java.util.ArrayList;
import java.lang.IllegalArgumentException;
import java.lang.ProcessBuilder;

public class RepairnatorProcessBuilder {
	private final ArrayList<String> cmdList = new ArrayList<String>();
	private static RepairnatorProcessBuilder repairnatorProcessBuilder;
	private String javaExec;
	private String jarLocation;
	private String gitUrl;
	private String gitBranch;
	private String gitOAuth;
	private String[] repairTools;
	private boolean createPR;
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
		this.createPR = true;
		return this;
	}


	public void checkValid() {
		if (this.javaExec == null || this.repairTools.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: java executable location is null");
		}

		if (this.jarLocation == null || this.repairTools.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: repairnator JAR location is null");
		}

		if (this.gitUrl== null || this.repairTools.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: no git url provided");
		}

		if (this.gitBranch == null || this.repairTools.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: no git branch provided");
		}

		if (this.repairTools == null || this.repairTools.equals("")) {
			throw new IllegalArgumentException("Repairnator Process building failed: no repair tools specified");
		}
	}

	public ProcessBuilder build() {
		this.checkValid();

		Config config = Config.getInstance();
		String mavenHome = config.getMavenHome();
		String workSpace = config.getTempDir().getAbsolutePath();
		String outputDir = config.getTempDir().getAbsolutePath();

		cmdList.add(this.javaExec);
		cmdList.add("-jar");
		cmdList.add("-Dlogs_dir=" + outputDir);
		cmdList.add(this.jarLocation);
		cmdList.add("--output");
		cmdList.add(outputDir);
		cmdList.add("--MavenHome");
		cmdList.add(mavenHome);
		cmdList.add("--workspace");
		cmdList.add(workSpace);
		cmdList.add("-b");
		cmdList.add("0");
		cmdList.add("--giturl");
		cmdList.add(this.gitUrl);
		cmdList.add("--gitbranch");
		cmdList.add(this.gitBranch);
		cmdList.add("--repairTools");
		cmdList.add(String.join(",",this.repairTools));
		cmdList.add("--noTravisRepair");
		if(!(config.getGitOAuth().equals("") || config.getGitOAuth() == null)) {
			cmdList.add("--ghOauth");
			cmdList.add(config.getGitOAuth());
			cmdList.add("--createPR");
		}

		return new ProcessBuilder(this.cmdList);
	}
}