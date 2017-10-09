package fr.inria.spirals.repairnator.process.step;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.main.AstorOutputStatus;
import fr.inria.main.evolution.AstorMain;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 17/08/2017.
 */
public class AstorRepair extends AbstractStep {

    public AstorRepair(ProjectInspector inspector) {
        super(inspector);
    }

    public AstorRepair(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    @Override
    protected void businessExecute() {
        this.getLogger().info("Start to repair using Astor");

        JobStatus jobStatus = this.getInspector().getJobStatus();
        List<String> astorPatches = new ArrayList<>();

        List<URL> classPath = this.inspector.getJobStatus().getRepairClassPath();
        File[] sources = this.inspector.getJobStatus().getRepairSourceDir();

        if (classPath != null && sources != null) {
            List<String> dependencies = new ArrayList<>();
            for (URL url : jobStatus.getRepairClassPath()) {
                if (url.getFile().endsWith(".jar")) {
                    dependencies.add(url.getPath());
                }
            }

            List<String> astorArgs = new ArrayList<>();
            astorArgs.add("-dependencies");
            astorArgs.add(StringUtils.join(dependencies,":"));

            astorArgs.add("-mode");
            astorArgs.add("jgenprog");

            astorArgs.add("-location");
            astorArgs.add(jobStatus.getFailingModulePath());

            astorArgs.add("-srcjavafolder");
            astorArgs.add(jobStatus.getRepairSourceDir()[0].getAbsolutePath());

            astorArgs.add("-stopfirst");
            astorArgs.add("true");

            astorArgs.add("-population");
            astorArgs.add("1");

            //astorArgs.add("-loglevel");
            //astorArgs.add("DEBUG");

            astorArgs.add("-parameters");
            astorArgs.add("timezone:Europe/Paris:maxnumbersolutions:3:limitbysuspicious:false:maxmodificationpoints:1000:javacompliancelevel:8:logfilepath:"+this.getInspector().getRepoLocalPath()+"/repairnator.astor.log");

            astorArgs.add("-maxtime");
            astorArgs.add("100");

            astorArgs.add("-seed");
            astorArgs.add("1");


            AstorMain astorMain = new AstorMain();

            AstorOutputStatus status;
            try {
                astorMain.execute(astorArgs.toArray(new String[0]));

                status = astorMain.getEngine().getOutputStatus();
                List<ProgramVariant> solutions = astorMain.getEngine().getSolutions();

                for (ProgramVariant pv : solutions) {
                    if (pv.isSolution()) {
                        astorPatches.add(pv.getPatchDiff());
                    }
                }
            } catch (Exception e) {
                status = AstorOutputStatus.ERROR;
                this.addStepError("Error while executing astor with args: "+ StringUtils.join(astorArgs,","), e);
            }

            jobStatus.addFileToPush("repairnator.astor.log");

            jobStatus.setAstorPatches(astorPatches);
            jobStatus.setAstorStatus(status);

            String jsonpath = astorMain.getEngine().getProjectFacade().getProperties().getWorkingDirRoot() + File.separator + ConfigurationProperties.getProperty("jsonoutputname") + ".json";

            File jsonResultFile = new File(jsonpath);
            if (jsonResultFile.exists()) {

                try {
                    FileUtils.copyFile(jsonResultFile, new File(this.getInspector().getRepoLocalPath()+"/repairnator.astor.results.json"));
                } catch (IOException e) {
                    this.addStepError("Error while moving astor JSON results", e);
                }

                JsonParser jsonParser = new JsonParser();
                try {
                    JsonElement root = jsonParser.parse(new FileReader(jsonResultFile));
                    this.getInspector().getJobStatus().setAstorResults(root);
                } catch (FileNotFoundException e) {
                    this.addStepError("Error while reading astor JSON results", e);
                }

                jobStatus.addFileToPush("repairnator.astor.results.json");
            }

            if (astorPatches.isEmpty()) {
                this.setPipelineState(PipelineState.ASTOR_NOTPATCHED);
            } else {
                this.setPipelineState(PipelineState.ASTOR_PATCHED);
            }
        }
    }
}
