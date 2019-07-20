package fr.inria.spirals.repairnator.realtime;

public class NopeRunner implements PipelineRunner<Boolean,String>{
    public NopeRunner(){}

    public Boolean submitBuild(String build){
        return true;
    }
}
