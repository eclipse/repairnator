package fr.inria.spirals.repairnator.process.step;

import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.main.evolution.AstorMain;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.commons.lang.StringUtils;

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
        List<ProgramVariant> astorPatches = new ArrayList<>();

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

        int totalMaxTime = 100;
        int counter = 0;
        for (FailureLocation location : jobStatus.getFailureLocations()) {
            counter++;
            int maxtime = (counter == jobStatus.getFailureLocations().size()-1) ? totalMaxTime : Math.round(totalMaxTime/2);

            List<String> locationArgs = new ArrayList<>(astorArgs);
            locationArgs.add("-maxtime");
            locationArgs.add(maxtime+"");
            locationArgs.add("-failing");
            locationArgs.add(location.getClassName());

            AstorMain astorMain = new AstorMain();
            try {
                astorMain.execute(locationArgs.toArray(new String[locationArgs.size()]));
                List<ProgramVariant> solutions = astorMain.getEngine().getSolutions();

                for (ProgramVariant pv : solutions) {
                    if (pv.isSolution()) {
                        astorPatches.add(pv);
                    }
                }
            } catch (Exception e) {
                this.addStepError("Error while executing astor with args: "+ StringUtils.join(locationArgs,","), e);
            }
        }

        jobStatus.setAstorPatches(astorPatches);

        if (astorPatches.isEmpty()) {
            this.setPipelineState(PipelineState.ASTOR_NOTPATCHED);
        } else {
            this.setPipelineState(PipelineState.ASTOR_PATCHED);
        }

    }
}
