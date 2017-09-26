package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.entities.TestsInformation;

import java.util.List;

/**
 * Created by urli on 22/02/2017.
 */
public abstract class JavaLogParser {

    protected List<TestsInformation> detailedResults;
    protected TestsInformation globalResults;


    public abstract TestsInformation parseLog(TravisFold outOfFolds);
    public abstract List<TestsInformation> parseDetailedLog(TravisFold outOfFolds);


}
