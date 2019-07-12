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
import fr.inria.spirals.repairnator.realtime.BuildRunner;
import fr.inria.spirals.repairnator.realtime.InspectBuilds;
import fr.inria.spirals.repairnator.realtime.InspectJobs;

/**
 * A class that counts the number of patches created since 
 * the rtscanner started running.
 * 
 * @author Benjamin Tellstrom on 2019-04-17
 *
 */

public class PullRequestCounter implements Runnable{
    
    // Sleep for this interval
    private static final int INTERVAL = 1800 * 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(PullRequestCounter.class);
    
    private int numberOfPRsToRunFor;
    private Bson PRFilter;
    private Bson buildFilter;
    private String mongodbHost;
    private String mongodbName;
    private EndProcessNotifier endProcessNotifier;
    private InspectBuilds inspectBuilds;
    private InspectJobs inspectJobs;
    private BuildRunner buildRunner;
    
    public PullRequestCounter(int numberOfPRsToRunFor, 
            String mongodbHost,
            String mongodbName,
            Date startDate,
            InspectBuilds inspectBuilds,
            InspectJobs inspectJobs,
            BuildRunner buildRunner) {
        // Set the variables
        this.numberOfPRsToRunFor = numberOfPRsToRunFor;
        this.mongodbHost = mongodbHost;
        this.mongodbName = mongodbName;
        
        // Create a filter that fetches each patch-document since the scanner started
        
        this.PRFilter = Filters.gte("date", startDate);
        this.buildFilter = Filters.and(
                Filters.gte("buildFinishedDate", startDate),
                Filters.eq("status", "PATCHED"));
    }
    
    public PullRequestCounter(int numberOfPRsToRunFor, 
            String mongodbHost,
            String mongodbName,
            Date startDate,
            InspectBuilds inspectBuilds,
            InspectJobs inspectJobs,
            BuildRunner buildRunner,
            EndProcessNotifier endProcessNotifier) {
        this(numberOfPRsToRunFor, mongodbHost, mongodbName, startDate,
                inspectBuilds, inspectJobs, buildRunner);
        this.endProcessNotifier = endProcessNotifier;
    }
    
    /**
     * Has the number of patches or patched builds exceeded the number
     * we intend them to run for?
     * @return yes or no (true or false)
     */
    public boolean keepRunning() {
        if(numberOfPRsToRunFor == 0) {
            return true;
        }
        MongoClient client = new MongoClient(new MongoClientURI(this.mongodbHost + "/" + this.mongodbName));
        MongoDatabase mongo = client.getDatabase(this.mongodbName);
        
        boolean run = this.numberOfPRsToRunFor > numberOfPRs(mongo);
        
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
    protected long numberOfPRs(MongoDatabase mongo) {
        return mongo.getCollection("pull-request").count(this.PRFilter);
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
            this.buildRunner.switchOff();
            if(this.endProcessNotifier != null) {
                this.endProcessNotifier.notifyEnd();
            }
        }
    }
    
}
