package fr.inria.spirals.repairnator.realtime.notifier;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.bson.conversions.Bson;
/**
 * A class for querying the mongo database and summarising the actions of Repairnator 
 * during a set interval.
 * @author benjamin
 *
 */
public class TimedSummaryNotifier implements Runnable {

    private static final long TIME_TO_SLEEP = 1 * 1000;

    protected static final String MONGO_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    protected static final SimpleDateFormat MONGO_DATE_FORMAT = new SimpleDateFormat(MONGO_UTC_FORMAT);

    protected List<NotifierEngine> engines;
    protected Calendar lastNotificationTime;
    protected Duration interval;
    protected Bson rtscannerFilter;
    protected Bson computeTestDirFilter;
    protected Bson patchesFilter;
    protected Map<String, Bson> toolFilters;
    protected String mongodbHost;
    protected String mongodbName;
    protected String[] repairTools;


    /**
     * Basic constructor. Sets all relevant fields.
     * @param engines
     * @param interval
     * @param tools
     * @param mongodbHost
     * @param mongodbName
     * @param notificationTime not yet implemented
     */
    public TimedSummaryNotifier(List<NotifierEngine> engines, Duration interval,
            String[] tools, String mongodbHost, String mongodbName, Date notificationTime) {
        this.engines = engines;
        this.interval = interval;
        this.mongodbHost = mongodbHost;
        this.mongodbName = mongodbName;
        this.repairTools = tools;
        this.lastNotificationTime = new GregorianCalendar();
        this.lastNotificationTime.setTime(notificationTime);

        Date previousDate = lastNotificationTime.getTime();
        
        this.rtscannerFilter = Filters.gte("dateWatched", previousDate);
        
        Pattern computeTestDir = Pattern.compile("NopolAllTests", Pattern.CASE_INSENSITIVE);    
        this.computeTestDirFilter = Filters.and(Filters.gte("dateBuildEnd", previousDate),
                Filters.eq("realStatus", computeTestDir));
        this.patchesFilter = Filters.gte("date", previousDate);

        this.toolFilters = new HashMap<String, Bson>();
        for (String tool : this.repairTools) {
            this.toolFilters.put(tool, Filters.and(Filters.gte("date", previousDate),
                    Filters.eq("toolname", tool)));
        }
    }

    public TimedSummaryNotifier(List<NotifierEngine> engines, Duration interval, String[] tools,
            String mongdbHost, String mongodbName) {
        this(engines, interval, tools, mongdbHost, mongodbName, (new GregorianCalendar()).getTime());
    }

    /**
     * Use the notifier engine to send an email to all addressess specified in the engine.
     * @param subject the subject line of the message
     * @param text the actual message
     */
    protected void notifyEngines(String subject, String text) {
        for (NotifierEngine engine : this.engines) {
            engine.notify(subject, text);
        }
    }

    @Override
    public void run() {
        while (true) {
            if (this.intervalHasPassed()) {
                MongoClient client = new MongoClient(
                        new MongoClientURI(this.mongodbHost + "/" + this.mongodbName));
                MongoDatabase mongo = client.getDatabase(this.mongodbName);

                updateFilters(lastNotificationTime.getTime());

                // Number of analyzed builds, rtscanner
                Iterator<Document> nrOfDocuments = queryDatabase("rtscanner", rtscannerFilter, mongo);
                int nrOfAnalyzedBuilds = nrOfObjects(nrOfDocuments);

                // Number of repair attempts, inspector ComputeTestDir will most likely help

                nrOfDocuments = queryDatabase("inspector", computeTestDirFilter, mongo);
                int nrOfRepairAttempts = nrOfObjects(nrOfDocuments);

                // Total number of patches, patches

                nrOfDocuments = queryDatabase("patches", patchesFilter, mongo);
                int nrOfPatches = nrOfObjects(nrOfDocuments);

                // Number of patches per tool, patches with an if

                int[] nrOfPatchesPerTool = new int[repairTools.length];

                for (int i = 0; i < repairTools.length; i++) {
                    nrOfDocuments = queryDatabase("patches", toolFilters.get(repairTools[i]), mongo);
                    nrOfPatchesPerTool[i] = nrOfObjects(nrOfDocuments);
                }
                
                Date now = new Date();
                
                String message = createMessage(nrOfAnalyzedBuilds, nrOfRepairAttempts, nrOfPatches, 
                        nrOfPatchesPerTool, now);
                updateLastNotificationTime(now);

                notifyEngines("Repairnator: Summary email", message);

                client.close();
            } else {
                try {
                    Thread.sleep(TIME_TO_SLEEP);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    /**
     * Checks whether the interval has passed since the last time a summary email was sent out.
     * @return
     */
    protected boolean intervalHasPassed() {
        return Duration.between(lastNotificationTime.getTime().toInstant(), (new Date()).toInstant())
                .compareTo(this.interval) > 0;
    }

    /**
     * Updates the lastNotificationTime
     * @param newTime the latest time a notification was sent out
     */
    void updateLastNotificationTime(Date newTime) {
        this.lastNotificationTime.setTime(newTime);
    }
    
    /**
     * Querys the mongodb for all documents fulfilling the requirements of the filter.
     * @param collectionName the collection to look int
     * @param filter the filer to apply
     * @param mongo the database to look in
     * @return all relevant documents
     */
    protected Iterator<Document> queryDatabase(String collectionName, Bson filter, MongoDatabase mongo) {
        FindIterable<Document> iterDoc = mongo.getCollection(collectionName).find(filter);
        return iterDoc.iterator();
    }

    /**
     * Update the filters such that the previousdate is instead used when querying
     * the database
     * 
     * @param previousDate
     *            the date that the filters should be based on
     */
    protected void updateFilters(Date previousDate) {
        
        this.rtscannerFilter = Filters.gte("dateWatched", previousDate);
        
        Pattern computeTestDir = Pattern.compile("NopolAllTests", Pattern.CASE_INSENSITIVE);
        this.computeTestDirFilter = Filters.and(Filters.gte("dateBuildEnd", previousDate),
                Filters.eq("realStatus", computeTestDir));
        this.patchesFilter = Filters.gte("date", previousDate);

        // Fetch the tools from config make map from name to filter
        this.toolFilters = new HashMap<String, Bson>();
        for (String tool : repairTools) {
            this.toolFilters.put(tool, Filters.and(Filters.gte("date", previousDate),
                    Filters.eq("toolname", tool)));
        }
    }

    /**
     * Count the number of documents in the given iterator
     * 
     * @param documents the iterator to count
     * @return number of objects in documents
     */
    protected int nrOfObjects(Iterator<Document> documents) {
        int iter = 0;
        while (documents.hasNext()) {
            iter += 1;
            documents.next();
        }
        return iter;
    }

    /**
     * Creates the message that is to be sent by this notifier
     * 
     * @param nrAnalyzedBuilds
     * @param nrRepairAttempts
     * @param nrOfPatches
     * @param tools
     * @param nrOfPatchesPerTool
     * @return the complete message
     */
    protected String createMessage(int nrAnalyzedBuilds, int nrRepairAttempts, int nrOfPatches, int[] nrOfPatchesPerTool, Date now) {

        String message = "Summary email from Repairnator. \n\n";
        message += "This summary contains the operations of Repairnator between "
                + this.lastNotificationTime.getTime().toString();
        message += " and ";
        message += now.toString() + ".\n";
        message += "Since the last summary Repairnator has: \n\n";

        // Number of analyzed builds, rtscanner
        message += "Number of analyzed builds: " + nrAnalyzedBuilds + "\n";

        // Number of repair attempts, inspector ComputeTestDir will most likely help
        message += "Number of repair attempts made: " + nrRepairAttempts + "\n";

        // Total number of patches, patches
        message += "Total number of patches: " + nrOfPatches + "\n";

        // Number of patches per tool, patches with an if

        for (int i = 0; i < repairTools.length; i++) {
            message += "Total number of patches found by " + repairTools[i] + ": " + nrOfPatchesPerTool[i] + "\n";
        }
        return message;
    }
}
