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

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import org.bson.Document;
import org.bson.conversions.Bson;

public class TimedSummaryNotifier implements Runnable {

    private static final long TIME_TO_SLEEP = 3600 * 1000;

    protected static final String MONGO_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    protected static final SimpleDateFormat MONGO_DATE_FORMAT = new SimpleDateFormat(MONGO_UTC_FORMAT);

    protected List<NotifierEngine> engines;
    protected Calendar lastNotificationTime;
    protected Duration interval;
    protected Bson rtscannerFilter;
    protected Bson computeTestDirFilter;
    protected Bson patchesFilter;
    protected Map<String, Bson> toolFilters;

    /**
     * Base, mostly here for testing§
     * 
     * @param engines
     * @param interval
     * @param notificationTime
     */
    public TimedSummaryNotifier(List<NotifierEngine> engines, Duration interval, Date notificationTime) {
        this.engines = engines;
        this.interval = interval;
        this.lastNotificationTime = new GregorianCalendar();
        this.lastNotificationTime.setTime(notificationTime);

        Date previousDate = lastNotificationTime.getTime();
        this.rtscannerFilter = Filters.gte("dateWatched", MONGO_DATE_FORMAT.format(previousDate));
        this.computeTestDirFilter = Filters.and(Filters.gte("dateBuildEnd", MONGO_DATE_FORMAT.format(previousDate)),
                Filters.eq("realStatus", "/NopolAllTests/i"));
        this.patchesFilter = Filters.gte("date", MONGO_DATE_FORMAT.format(previousDate));

        // Fetch the tools from config make map from name to filter
        RepairnatorConfig instance = RepairnatorConfig.getInstance();
        this.toolFilters = new HashMap<String, Bson>();
        for (String tool : instance.getRepairTools()) {
            this.toolFilters.put(tool, Filters.and(Filters.gte("date", MONGO_DATE_FORMAT.format(previousDate)),
                    Filters.eq("toolname", tool)));
        }
    }

    /**
     * Constructor for when no specific time of day is wanted.
     * 
     * @param engines
     *            recipients
     * @param mongo
     *            the database to query
     * @param interval
     *            the interval between notifications
     */
    public TimedSummaryNotifier(List<NotifierEngine> engines, Duration interval) {
        this(engines, interval, (new GregorianCalendar()).getTime());
    }

    protected void notifyEngines(String subject, String text) {
        for (NotifierEngine engine : this.engines) {
            engine.notify(subject, text);
        }
    }

    @Override
    public void run() {
        while (true) {
            if (this.intervalHasPassed()) {

                RepairnatorConfig config = RepairnatorConfig.getInstance();

                MongoClient client = new MongoClient(
                        new MongoClientURI(config.getMongodbHost() + "/" + config.getMongodbName()));
                MongoDatabase mongo = client.getDatabase(config.getMongodbName());

                updateFilters(lastNotificationTime.getTime());
                updateCalendar(new Date());

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

                String[] tools = (String[]) config.getRepairTools().toArray();
                int[] nrOfPatchesPerTool = new int[tools.length];

                for (int i = 0; i < tools.length; i++) {
                    nrOfDocuments = queryDatabase("patches", toolFilters.get(tools[i]), mongo);
                    nrOfPatchesPerTool[i] = nrOfObjects(nrOfDocuments);
                }

                String message = createMessage(nrOfAnalyzedBuilds, nrOfRepairAttempts, nrOfPatches, tools,
                        nrOfPatchesPerTool);

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

    protected boolean intervalHasPassed() {
        return Duration.between(lastNotificationTime.getTime().toInstant(), (new Date()).toInstant())
                .compareTo(this.interval) < 0;
    }

    /**
     * Simple method for updating the calendar value
     */
    protected void updateCalendar(Date newTime) {
        this.lastNotificationTime.setTime(newTime);
    }

    /**
     * Query the database for the specfied collection given the special filter
     * 
     * @param collectionName
     *            the collection to query
     * @param filter
     *            the filter to apply to the collectionquery
     * @return
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

        this.rtscannerFilter = Filters.gte("dateWatched", MONGO_DATE_FORMAT.format(previousDate));
        this.computeTestDirFilter = Filters.and(Filters.gte("dateBuildEnd", MONGO_DATE_FORMAT.format(previousDate)),
                Filters.eq("realStatus", "/NopolAllTests/i"));
        this.patchesFilter = Filters.gte("date", MONGO_DATE_FORMAT.format(previousDate));

        // Fetch the tools from config make map from name to filter
        RepairnatorConfig instance = RepairnatorConfig.getInstance();
        this.toolFilters = new HashMap<String, Bson>();
        for (String tool : instance.getRepairTools()) {
            this.toolFilters.put(tool, Filters.and(Filters.gte("date", MONGO_DATE_FORMAT.format(previousDate)),
                    Filters.eq("toolname", tool)));
        }
    }

    /**
     * Count the number of documents in the given iterator
     * 
     * @param documents
     *            the tierator to count
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
    protected String createMessage(int nrAnalyzedBuilds, int nrRepairAttempts, int nrOfPatches, String[] tools,
            int[] nrOfPatchesPerTool) {

        String message = "Summary email from Repairnator. \n\n";
        message += "This summary contains the operations of Repairnator between "
                + this.lastNotificationTime.getTime().toString();
        message += " and ";
        message += this.lastNotificationTime.getTime().toString() + ".\n";
        message += "Since the last summary Repairnator has: \n \n";

        // Number of analyzed builds, rtscanner
        message += "Number of analyzed builds: " + nrAnalyzedBuilds + " \n";

        // Number of repair attempts, inspector ComputeTestDir will most likely help
        message += "Number of repair attempts made: " + nrRepairAttempts + "\n";

        // Total number of patches, patches
        message += "Total number of patches: " + nrOfPatches + "\n";

        // Number of patches per tool, patches with an if
        tools = (String[]) RepairnatorConfig.getInstance().getRepairTools().toArray();

        for (int i = 0; i < tools.length; i++) {
            message += "Total number of patches found by " + tools[i] + ": " + nrOfPatchesPerTool[i] + "\n";
        }
        return message;
    }
}
