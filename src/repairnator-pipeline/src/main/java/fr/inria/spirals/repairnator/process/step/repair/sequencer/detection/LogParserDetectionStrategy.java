package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.step.logParser.Element;
import fr.inria.spirals.repairnator.process.step.logParser.LogParser;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.SequencerRepair;
import org.slf4j.Logger;

import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class LogParserDetectionStrategy implements DetectionStrategy {

    LogParser parser;
    MavenPatchTester mavenTester;

    void setMavenTester(MavenPatchTester tester){
        this.mavenTester = tester;
    }


    public LogParserDetectionStrategy(){
        parser = new LogParser();
    }

    @Override
    public List<ModificationPoint> detect(SequencerRepair repairStep) {

        List<String> buildLog = repairStep.getInspector().getBuildLog();

        parser.parse(buildLog);

        List<Element> errors = parser.getErrors();

        List<ModificationPoint> points = errors.stream()
                .map( point -> new ModificationPoint(Paths.get(point.<String>get("file")), point.<Integer>get("line")))
                .distinct()
                .collect(Collectors.toList());

        return points;
    }

    @Override
    public boolean validate(RepairPatch patch) {
        Properties properties = new Properties();
        properties.setProperty(MavenHelper.SKIP_TEST_PROPERTY, "true");
        return mavenTester.apply(patch, "package", properties);
    }

    @Override
    public void setup(ProjectInspector inspector, String pom, Logger logger) {
        setMavenTester(new MavenPatchTester(inspector, pom, logger));
    }
}
