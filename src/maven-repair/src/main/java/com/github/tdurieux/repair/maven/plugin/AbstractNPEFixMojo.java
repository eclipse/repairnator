package com.github.tdurieux.repair.maven.plugin;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.InspectorFactory;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractNPEFixMojo extends AbstractRepairMojo {

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/npefix", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/npefix", property = "resultDir", required = true)
    private File resultDirectory;

    @Parameter(defaultValue = "exploration", property = "selector", required = true)
    protected String selector;

    @Parameter(defaultValue = "100", property = "laps", required = true)
    protected int nbIteration;

    @Parameter(defaultValue = "class", property = "scope", required = true)
    protected String scope;

    @Parameter(defaultValue = "default", property = "strategy", required = true)
    protected String repairStrategy;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    private File patchesJson;

    protected AbstractRepairStep repairStep;

    public void execute() throws MojoExecutionException {
        File tmpRepairnatorDir = com.google.common.io.Files.createTempDir();
        RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(repairStep.getRepairToolName()));
        RepairnatorConfig.getInstance().setOutputPath(tmpRepairnatorDir.getAbsolutePath());
        RepairnatorConfig.getInstance().setLocalMavenRepository(localRepository.getBasedir());
        InspectorFactory.getMavenInspector(project.getBasedir().getAbsolutePath(), Collections.singletonList(repairStep), null).run();

        List<File> patches = Arrays.asList(tmpRepairnatorDir.listFiles(((dir, name) -> name.startsWith("patches") && name.endsWith(".json"))));

        if (patches.size() == 0) {
            this.getLog().error("No patches have been found by " + repairStep.getRepairToolName());
        } else {
            try {
                resultDirectory.mkdirs();
                patchesJson = new File(resultDirectory.getAbsolutePath() + "/" + patches.get(0).getName());
                patchesJson.createNewFile();
                com.google.common.io.Files.copy(patches.get(0), patchesJson);
            } catch (IOException e) {
                this.getLog().error(e);
            }
        }

        cleanupRepairnatorFiles();
    }

    private void cleanupRepairnatorFiles() {
        try {
            FileUtils.forceDelete(new File(project.getBasedir().getAbsolutePath() + "/repairnator.json"));
            FileUtils.forceDelete(new File(project.getBasedir().getAbsolutePath() + "/classpath.info"));
            FileUtils.forceDelete(new File(project.getBasedir().getAbsolutePath() + "/repairnator.maven.buildproject.log"));
            FileUtils.forceDelete(new File(project.getBasedir().getAbsolutePath() + "/repairnator.maven.computeclasspath.log"));
            FileUtils.forceDelete(new File(project.getBasedir().getAbsolutePath() + "/repairnator.maven.testproject.log"));
            FileUtils.deleteDirectory(new File(project.getBasedir().getAbsolutePath() + "/_topush"));
            FileUtils.deleteDirectory(new File(project.getBasedir().getAbsolutePath() + "/npefix-bin"));
            FileUtils.deleteDirectory(new File(project.getBasedir().getAbsolutePath() + "/repairnatorPatches"));
        } catch (IOException e) {
            this.getLog().error(e);
        }
    }

    public File getResultDirectory() {
        return resultDirectory;
    }

}