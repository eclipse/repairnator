package fr.inria.spirals.repairnator.process.step.repair.sequencer.detection;

import fr.inria.spirals.repairnator.process.step.logParser.Element;
import fr.inria.spirals.repairnator.process.step.logParser.LogParser;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.SequencerRepair;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class LogParserDetectionStrategy implements DetectionStrategy {

    LogParser parser;

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
}
