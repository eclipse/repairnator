package fr.inria.spirals.repairnator.process.step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by urli on 08/02/2017.
 */
public class ComputeSourceDir extends AbstractStep {
    private static final String DEFAULT_SRC_DIR = "/src/main/java";
    private static final String COMPUTE_TOTAL_CLOC = "cloc --json --vcs=git .";

    private boolean allModules;
    private Set<File> visitedFiles = new HashSet<>();

    public ComputeSourceDir(ProjectInspector inspector, boolean allModules) {
        super(inspector);
        this.allModules = allModules;
    }

    public ComputeSourceDir(ProjectInspector inspector, String name, boolean allModules) {
        super(inspector, name);
        this.allModules = allModules;
    }

    private File[] searchForSourcesDirectory(String incriminatedModulePath, boolean rootCall) {
        List<File> result = new ArrayList<File>();
        File defaultSourceDir = new File(incriminatedModulePath + DEFAULT_SRC_DIR);

        if (defaultSourceDir.exists()) {
            result.add(defaultSourceDir);
            if (!this.allModules) {
                return result.toArray(new File[result.size()]);
            }
        }

        this.getLogger().debug("The default source directory (" + defaultSourceDir.getPath()
                + ") does not exists. Try to read pom.xml to get informations.");
        File pomIncriminatedModule = new File(incriminatedModulePath + "/pom.xml");

        if (!pomIncriminatedModule.exists()) {
            pomIncriminatedModule = new File(this.getPom());
        }

        if (this.visitedFiles.contains(pomIncriminatedModule)) {
            this.getLogger().info("It seems we are entering in a loop while searching the source dir. The following file has already been visited: "+pomIncriminatedModule.getAbsolutePath());
            return result.toArray(new File[0]);
        } else {
            this.visitedFiles.add(pomIncriminatedModule);
        }

        try {
            Model model = MavenHelper.readPomXml(pomIncriminatedModule, this.inspector.getM2LocalPath());

            Build buildSection = model.getBuild();

            if (buildSection != null) {
                String pathSrcDirFromPom = model.getBuild().getSourceDirectory();

                File srcDirFromPom = new File(pathSrcDirFromPom);

                if (srcDirFromPom.exists()) {
                    result.add(srcDirFromPom);
                    return result.toArray(new File[result.size()]);
                }

                this.getLogger().debug("The source directory given in pom.xml (" + pathSrcDirFromPom
                        + ") does not exists. Try to get source dir from all modules if multimodule.");
            } else {
                this.getLogger().debug(
                        "Build section does not exists in this pom.xml. Try to get source dir from all modules.");
            }

            for (String module : model.getModules()) {
                File[] srcDir = this.searchForSourcesDirectory(pomIncriminatedModule.getParent() + File.separator + module,
                        false);
                if (srcDir != null) {
                    result.addAll(Arrays.asList(srcDir));
                }
            }

            if (result.size() > 0) {
                return result.toArray(new File[result.size()]);
            }

            if (model.getParent() != null && rootCall) {
                String relativePath = "../pom.xml";

                if (model.getParent().getRelativePath() != null) {
                    relativePath = model.getParent().getRelativePath();
                }

                File parentPomXml = new File(incriminatedModulePath + File.separator + relativePath);

                if (parentPomXml.exists()) {
                    File[] srcDir = this.searchForSourcesDirectory(parentPomXml.getParent(),false);
                    if (srcDir != null) {
                        result.addAll(Arrays.asList(srcDir));
                    }

                    if (result.size() > 0) {
                        return result.toArray(new File[result.size()]);
                    }
                }
            }


        } catch (ModelBuildingException e) {
            this.addStepError("Error while building pom.xml model: " + e);
        }

        this.addStepError(
                "Source directory is not at default location or specified in build section and no parent can be found.");
        return null;
    }

    private void computeMetricsOnSources(File[] sources) {
        int totalAppFiles = 0;
        if (sources != null && sources.length > 0) {
            for (File f : sources) {
                int nbFile = FileUtils.listFiles(f, new String[] {"java"}, true).size();
                totalAppFiles += nbFile;
            }
            this.inspector.getJobStatus().getMetrics().setNbFileApp(totalAppFiles);
        }
    }

    private void computeMetricsOnCompleteRepo() {
        this.getLogger().debug("Compute the line of code of the project");
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh","-c",COMPUTE_TOTAL_CLOC)
                .directory(new File(this.inspector.getRepoLocalPath()));

        try {
            Process p = processBuilder.start();
            BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();

            this.getLogger().debug("Get result from cloc process...");
            String processReturn = "";
            String line;
            while (stdin.ready() && (line = stdin.readLine()) != null) {
                processReturn += line;
            }

            Gson gson = new GsonBuilder().create();
            JsonObject json = gson.fromJson(processReturn, JsonObject.class);

            this.inspector.getJobStatus().getMetrics().setSizeProjectLOC(json);
        } catch (IOException | InterruptedException e) {
            this.getLogger().error("Error while computing metrics on source code of the whole repo.", e);
        }
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Computing the source directory ...");
        String incriminatedModule = (this.allModules) ? this.inspector.getRepoLocalPath() : this.inspector.getJobStatus().getFailingModulePath();

        File[] sources = this.searchForSourcesDirectory(incriminatedModule, true);

        if (allModules) {
            this.computeMetricsOnCompleteRepo();
            this.computeMetricsOnSources(sources);
        }

        if (sources == null) {
            this.addStepError("Fail to find the sources directory.");
            this.setPipelineState(PipelineState.SOURCEDIRNOTCOMPUTED);
            this.inspector.getJobStatus().setRepairSourceDir(null);
        } else {
            this.inspector.getJobStatus().setRepairSourceDir(sources);
            this.setPipelineState(PipelineState.SOURCEDIRCOMPUTED);
        }
    }

}
