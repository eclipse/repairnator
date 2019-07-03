package fr.inria.spirals.repairnator.process.step;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.POMModifier;
import fr.inria.spirals.repairnator.states.PipelineState;

public class AddExperimentalPluginRepo extends AbstractStep {

    private String repoId;
    private String repoName;
    private String repoUrl;
    
    public AddExperimentalPluginRepo(ProjectInspector inspector, String repoId, String repoName, String repoUrl) {
        super(inspector, true);
        this.repoId = repoId;
        this.repoName = repoName;
        this.repoUrl = repoUrl;
    }
    
    public AddExperimentalPluginRepo(ProjectInspector inspector, boolean blockingStep, String stepName, String repoId, String repoName, String repoUrl) {
        super(inspector, blockingStep, stepName);
    }
    @Override
    protected StepStatus businessExecute() {
        this.getLogger().info("Adding the experimental plugin repository to the pom-file.");
        
        try {
            POMModifier.addPluginRepo(this.getPom(), this.repoId, this.repoName, this.repoUrl);
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            this.getLogger().error("Something went wrong when attempting to change the pom-file.");
            this.addStepError("The pom-file could not be modified.");
            return StepStatus.buildError(this, PipelineState.PLUGINSNOTCOMPUTED);
        }
        return StepStatus.buildSuccess(this);
    }

}
