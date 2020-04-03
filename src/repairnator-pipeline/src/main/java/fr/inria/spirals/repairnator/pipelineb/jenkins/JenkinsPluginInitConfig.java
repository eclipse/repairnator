package fr.inria.spirals.repairnator.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAP;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.LauncherUtils;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.LauncherType;

import java.io.File;

/* Config init behavior for repairing as Jenkins Plugin */
public class JenkinsPluginInitConfig implements IInitConfig {
    private static Logger LOGGER = LoggerFactory.getLogger(DefaultInitConfig.class);

    protected static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    @Override
    /* Extending Github initConfig method */
    public void initConfigWithJSAP(JSAP jsap, String[] inputArgs) {
        GithubInitConfig ghInitConfig = new GithubInitConfig();
        ghInitConfig.initConfigWithJSAP(jsap,inputArgs);


        LOGGER.info("Repairnator will be running for - GitUrl: " + this.getConfig().getGitRepositoryUrl() + " --  GitBranch: " + this.getConfig().getGitRepositoryBranch() + " -- GitCommit: " + this.getConfig().getGitCommitHash());
        File f = new File(System.getProperty("java.class.path"));
        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("java.class.path",f.getAbsolutePath());
        System.setProperty("user.dir",this.getConfig().getWorkspacePath());
        
        LOGGER.info("user.dir=" + System.getProperty("user.dir"));
        LOGGER.info("java.class.path=" + System.getProperty("java.class.path"));
        this.getConfig().setRunId("1234");
        this.getConfig().setGithubUserEmail("noreply@github.com");
        this.getConfig().setGithubUserName("repairnator");

        System.setProperty("user.dir",oldUserDir);
    }
}