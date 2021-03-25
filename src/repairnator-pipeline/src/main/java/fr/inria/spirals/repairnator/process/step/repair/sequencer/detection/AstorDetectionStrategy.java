package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.astor.approaches._3sfix.ZmEngine;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.main.CommandSummary;
import fr.inria.main.evolution.AstorMain;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.SequencerRepair;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AstorDetectionStrategy implements DetectionStrategy {

    MavenPatchTester mavenTester;

    String suspicionThreshold = "0.5";
    String suspiciousCandidates = "100";

    void setMavenTester(MavenPatchTester tester){
        this.mavenTester = tester;
    }

    @Override
    public List<ModificationPoint> detect(SequencerRepair repairStep) {

        ProjectInspector inspector = repairStep.getInspector();
        JobStatus jobStatus = inspector.getJobStatus();
        List<URL> repairClasspath = jobStatus.getRepairClassPath();

        // prepare CommandSummary
        CommandSummary cs = new CommandSummary();
        List<String> dependencies = new ArrayList<>();

        for (URL url : repairClasspath) {
            if (url.getFile().endsWith(".jar")) {
                dependencies.add(url.getPath());
            }
        }
//        cs.command.put("-loglevel", "DEBUG");
        cs.command.put("-mode", "custom");
        cs.command.put("-dependencies", StringUtils.join(dependencies,":"));
        cs.command.put("-location", jobStatus.getFailingModulePath());
        cs.command.put("-flthreshold", suspicionThreshold);
        cs.command.put("-maxgen", "0");
        cs.command.put("-javacompliancelevel", "8");
        cs.command.put("-customengine", ZmEngine.class.getCanonicalName());
        cs.command.put("-parameters", "disablelog:false:logtestexecution:true:logfilepath:"
                + inspector.getRepoLocalPath()
                + "/repairnator." + "sequencerRepair" + ".log");
        cs.command.put("-maxsuspcandidates", suspiciousCandidates);

        AstorMain astorMain = new AstorMain();
        try {
            astorMain.execute(cs.flat());
        } catch (Exception e) {
            repairStep.addStepError("Got exception when running SequencerRepair: ", e);
            //ignore Astor/GZoltar error, return empty list and exit.
            return new ArrayList<>();
        }
        // construct ZmEngine
        ZmEngine zmengine = (ZmEngine) astorMain.getEngine();
        List<SuspiciousModificationPoint> suspicious = zmengine.getSuspicious();

        return suspicious.stream()
                .map(ModificationPoint::CreateFrom)
                .collect(Collectors.toList()
        );
    }

    @Override
    public boolean validate(RepairPatch patch) {
        Properties properties = new Properties();
        return mavenTester.apply(patch, "clean test", properties);
    }

    @Override
    public void setup(ProjectInspector inspector, String pom, Logger logger) {
        setMavenTester(new MavenPatchTester(inspector, pom, logger));
    }
}
