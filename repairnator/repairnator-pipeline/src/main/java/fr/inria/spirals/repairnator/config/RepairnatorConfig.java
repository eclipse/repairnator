package fr.inria.spirals.repairnator.config;

import fr.inria.spirals.repairnator.FileMode;
import fr.inria.spirals.repairnator.LauncherMode;

/**
 * Created by urli on 08/03/2017.
 */
public class RepairnatorConfig {
    private FileMode fileMode;
    private LauncherMode launcherMode;
    private int lookupHours;
    private boolean clean;
    private boolean push;
    private String workspacePath;
    private String z3solverPath;
    private boolean serializeJson;
    private String jsonOutputPath;
    private String pushRemoteRepo;

    private static RepairnatorConfig instance;

    private RepairnatorConfig() {}

    public static RepairnatorConfig getInstance() throws RepairnatorConfigException {
        if (instance == null) {
            instance = new RepairnatorConfig();
            RepairnatorConfigReader configReader = new RepairnatorConfigReader();
            configReader.readConfigFile(instance);
        }
        return instance;
    }

    public FileMode getFileMode() {
        return fileMode;
    }

    public void setFileMode(FileMode fileMode) {
        this.fileMode = fileMode;
    }

    public LauncherMode getLauncherMode() {
        return launcherMode;
    }

    public void setLauncherMode(LauncherMode launcherMode) {
        this.launcherMode = launcherMode;
    }

    public int getLookupHours() {
        return lookupHours;
    }

    public void setLookupHours(int lookupHours) {
        this.lookupHours = lookupHours;
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public boolean isPush() {
        return push;
    }

    public void setPush(boolean push) {
        this.push = push;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public String getZ3solverPath() {
        return z3solverPath;
    }

    public void setZ3solverPath(String z3solverPath) {
        this.z3solverPath = z3solverPath;
    }

    public boolean isSerializeJson() {
        return serializeJson;
    }

    public void setSerializeJson(boolean serializeJson) {
        this.serializeJson = serializeJson;
    }

    public String getJsonOutputPath() {
        return jsonOutputPath;
    }

    public void setJsonOutputPath(String jsonOutputPath) {
        this.jsonOutputPath = jsonOutputPath;
    }

    public String getPushRemoteRepo() {
        return pushRemoteRepo;
    }

    public void setPushRemoteRepo(String pushRemoteRepo) {
        this.pushRemoteRepo = pushRemoteRepo;
    }

    @Override
    public String toString() {
        return "RepairnatorConfig{" +
                "fileMode=" + fileMode +
                ", launcherMode=" + launcherMode +
                ", lookupHours=" + lookupHours +
                ", clean=" + clean +
                ", push=" + push +
                ", workspacePath='" + workspacePath + '\'' +
                ", z3solverPath='" + z3solverPath + '\'' +
                ", serializeJson=" + serializeJson +
                ", jsonOutputPath='" + jsonOutputPath + '\'' +
                ", pushRemoteRepo='" + pushRemoteRepo + '\'' +
                '}';
    }
}
