package fr.inria.spirals.repairnator.serializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;

public class PullRequestSerializer4GitRepository extends AbstractDataSerializer {
    
    public PullRequestSerializer4GitRepository(List<SerializerEngine> engines, ProjectInspector inspector) {
        super(engines, SerializerType.PULL_REQUEST, inspector);
    }
    
    private List<Object> serializeAsList(ProjectInspector inspector, String prUrl) {
    	List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(inspector.getProjectIdToBeInspected());
        dataCol.add(inspector.getGitSlug());
        dataCol.add(DateUtils.formatCompleteDate(new Date()));
        dataCol.add(prUrl);
        
        return dataCol;
    }
    
    private JsonElement serializeAsJson(ProjectInspector inspector, String prUrl) {
        JsonObject result = new JsonObject();
        result.addProperty("buildId", inspector.getProjectIdToBeInspected());
        result.addProperty("repositoryName", inspector.getGitSlug());
        result.addProperty("dateOfPRStr", DateUtils.formatCompleteDate(new Date()));
        this.addDate(result, "dateOfPR", new Date());
        result.addProperty("PRurl", prUrl);
        
        return result;
        
    }
    
    @Override
    public void serialize() {
        List<SerializedData> allData = new ArrayList<>();
        
        for(String prUrl: inspector.getJobStatus().getPRCreated()) {
             allData.add(new SerializedData(this.serializeAsList(inspector, prUrl), this.serializeAsJson(inspector, prUrl)));
        }
        
        if(!allData.isEmpty()) {
            for(SerializerEngine engine: this.getEngines()) {
                engine.serialize(allData, this.getType());
            }
        }
    }
}
