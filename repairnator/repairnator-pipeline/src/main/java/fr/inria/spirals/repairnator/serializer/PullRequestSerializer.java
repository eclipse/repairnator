package fr.inria.spirals.repairnator.serializer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.utils.DateUtils;

/**
 * Serializes data regarding pull requests created by Repairnator. Will be used when 
 * documenting the pull requests in a document database such as Mongo DB.
 *
 * The data stored in the generated documents and lists are:
 *      1. The build id of the travis-ci build repairnator was executed on.
 *      2. The slug of the github repository for which the pull request was created (user/organisation + projectname)
 *      3. The date when the travis-ci build finished.
 *      4. The date on which the pull request was created.
 *      5. The url of the created pull request.
 *
 * @author Benjamin Tellström
 * Created on 11/07-2019
 *
 */

public class PullRequestSerializer extends AbstractDataSerializer {
    
    public PullRequestSerializer(List<SerializerEngine> engines, ProjectInspector inspector) {
        super(engines, SerializerType.PULL_REQUEST, inspector);
    }
    

    private List<Object> serializeAsList(ProjectInspector inspector, String prUrl) {
        Build build = inspector.getBuggyBuild();
        
        List<Object> dataCol = new ArrayList<Object>();
        dataCol.add(build.getId() + "");
        dataCol.add(build.getRepository().getSlug());
        dataCol.add(DateUtils.formatCompleteDate(build.getFinishedAt()));
        dataCol.add(DateUtils.formatOnlyDay(build.getFinishedAt()));
        dataCol.add(DateUtils.formatCompleteDate(new Date()));
        dataCol.add(prUrl);
        
        return dataCol;
    }
    
    private JsonElement serializeAsJson(ProjectInspector inspector, String prUrl) {
        Build build = inspector.getBuggyBuild();
        
        JsonObject result = new JsonObject();
        result.addProperty("buildId", build.getId());
        result.addProperty("repositoryName", build.getRepository().getSlug());
        result.addProperty("buildFinishedDateStr", DateUtils.formatCompleteDate(build.getFinishedAt()));
        this.addDate(result, "buildFinishedDate", build.getFinishedAt());
        result.addProperty("dateOfPRStr", DateUtils.formatCompleteDate(new Date()));
        this.addDate(result, "dateOfPR", new Date());
        result.addProperty("PRurl", prUrl);
        
        return result;
        
    }
    
    
    public void serializeData(ProjectInspector inspector) {
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
