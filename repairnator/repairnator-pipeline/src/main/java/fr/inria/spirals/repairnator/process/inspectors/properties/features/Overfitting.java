package fr.inria.spirals.repairnator.process.inspectors.properties.features;

import fr.inria.prophet4j.utility.CodeDiffer;
import fr.inria.prophet4j.utility.Option;
import fr.inria.prophet4j.utility.Support;
import fr.inria.prophet4j.utility.Option.DataOption;
import fr.inria.prophet4j.utility.Option.FeatureOption;
import fr.inria.prophet4j.utility.Option.LearnerOption;
import fr.inria.prophet4j.utility.Option.PatchOption;
import fr.inria.prophet4j.utility.Structure.FeatureMatrix;
import fr.inria.prophet4j.utility.Structure.ParameterVector;
import fr.inria.prophet4j.utility.Support.DirType;

import java.io.File;
import java.util.List;

public class Overfitting {
    private CodeDiffer codeDiffer;
    private ParameterVector parameterVector;

    public Overfitting(Features features) {
        Option option = new Option();
        option.dataOption = DataOption.BUG_DOT_JAR_MINUS_MATH;
        option.patchOption = PatchOption.BUG_DOT_JAR_MINUS_MATH;
        switch (features) {
            case P4J:
                option.featureOption = FeatureOption.ORIGINAL;
                break;
            case S4R:
                option.featureOption = FeatureOption.S4R;
                break;
        }
        option.learnerOption = LearnerOption.CROSS_ENTROPY;
        this.codeDiffer = new CodeDiffer(false, option);
        this.parameterVector = new ParameterVector(option.featureOption);
        String parameterFilePath = Support.getFilePath(DirType.PARAMETER_DIR, option) + "ParameterVector";
        this.parameterVector.load(parameterFilePath);
    }

    public double computeScore(File buggyFile, File patchedFile) {
        List<FeatureMatrix> featureMatrices = this.codeDiffer.runByGenerator(buggyFile, patchedFile);
        return featureMatrices.size() == 1 ? ((FeatureMatrix) featureMatrices.get(0)).score(this.parameterVector) : Double.POSITIVE_INFINITY;
    }
}
