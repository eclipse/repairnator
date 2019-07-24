package fr.inria.spirals.repairnator.realtime.counter;

import java.util.Date;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import fr.inria.spirals.repairnator.notifier.EndProcessNotifier;
import fr.inria.spirals.repairnator.realtime.DockerPipelineRunner;
import fr.inria.spirals.repairnator.realtime.InspectBuilds;
import fr.inria.spirals.repairnator.realtime.InspectJobs;
/**
 * A class that counts the number of patches created since 
 * the rtscanner started running.
 * 
 * @author Benjamin Tellstrom on 2019-04-17
 *
 */

public class PatchCounter implements Runnable{
    
    // Sleep for this interval
    private static final int INTERVAL = 1800 * 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(PatchCounter.class);
    private Boolean kubernetesmode = false;
    
    private int numberOfPatchesToRunFor;
    private Bson patchesFilter;
    private Bson buildFilter;
    private String mongodbHost;
    private String mongodbName;
    private EndProcessNotifier endProcessNotifier;
    private InspectBuilds inspectBuilds;
    private InspectJobs inspectJobs;
    private DockerPipelineRunner dockerPipelineRunner;

    public PatchCounter(int numberOfPatchesToRunFor, 
            String mongodbHost,
            String mongodbName,
            Date startDate,
            InspectBuilds inspectBuilds,
            InspectJobs inspectJobs,
            DockerPipelineRunner dockerPipelineRunner) {
        // Set the variables
        this.numberOfPatchesToRunFor = numberOfPatchesToRunFor;
        this.mongodbHost = mongodbHost;
        this.mongodbName = mongodbName;
        
        // Create a filter that fetches each patch-document since the scanner started
        
        this.patchesFilter = Filters.gte("date", startDate);
        this.buildFilter = Filters.and(
                Filters.gte("buildFinishedDate", startDate),
                Filters.eq("status", "PATCHED"));
        this.dockerPipelineRunner = dockerPipelineRunner;
    }
        
    public PatchCounter(int numberOfPatchesToRunFor, 
            String mongodbHost,
            String mongodbName,
            Date startDate,
            InspectBuilds inspectBuilds,
            InspectJobs inspectJobs,
            DockerPipelineRunner dockerPipelineRunner,
            EndProcessNotifier endProcessNotifier) {
        this(numberOfPatchesToRunFor, mongodbHost, mongodbName, startDate,
                inspectBuilds, inspectJobs, dockerPipelineRunner);
        this.endProcessNotifier = endProcessNotifier;
    }
    
    public void setKubernetesMode(Boolean kubernetesmode) {
        this.kubernetesmode = kubernetesmode;
    }

    /**
     * Has the number of patches or patched builds exceeded the number
     * we intend them to run for?
     * @return yes or no (true or false)
     */
    public boolean keepRunning() {
        if(numberOfPatchesToRunFor == 0) {
            return true;
        }
        MongoClient client = new MongoClient(new MongoClientURI(this.mongodbHost + "/" + this.mongodbName));
        MongoDatabase mongo = client.getDatabase(this.mongodbName);
        
        boolean run = this.numberOfPatchesToRunFor > numberOfBuildsPatched(mongo);
        
        client.close();
        return run;
        // return this.numberOfPatchesToRunFor > numberOfPatches(mongo);
    }
    
    /**
     * Count the total number of builds that have been patched. Probably what we want?
     * @param mongo the database to search through
     * @return number of builds patched
     */
    protected long numberOfBuildsPatched(MongoDatabase mongo) {
        return mongo.getCollection("inspector").count(this.buildFilter);
    }
    
    /**
     * Count the total number of patches generated
     * @param mongo the database to search through
     * @return number of builds patched
     */
    protected long numberOfPatches(MongoDatabase mongo) {
        return mongo.getCollection("inspector").count(this.patchesFilter);
    }

    @Override
    public void run() {
        if(this.keepRunning()) {
            try {
                Thread.sleep(INTERVAL);
            } catch (InterruptedException e) {              
                LOGGER.warn("Sleep interrupted.");
            }
        } else {
            LOGGER.info("The process will now stop.");
            this.inspectBuilds.switchOff();
            this.inspectJobs.switchOff();
            if (!kubernetesmode) {
                this.dockerPipelineRunner.switchOff();
            }
            if(this.endProcessNotifier != null) {
                this.endProcessNotifier.notifyEnd();
            }
        }
    }    
}
