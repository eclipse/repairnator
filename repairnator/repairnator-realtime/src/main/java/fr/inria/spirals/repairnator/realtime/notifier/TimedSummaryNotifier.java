package fr.inria.spirals.repairnator.realtime.notifier;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.conversions.Bson; 

public class TimedSummaryNotifier implements Runnable{
    
    private static final String MONGO_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat MONGO_DATE_FORMAT = new SimpleDateFormat(MONGO_UTC_FORMAT);
    private static final long TIME_TO_SLEEP = 3600 * 1000;
    private static final String[] COLLECTIONS_TO_QUERY = {"patches", 
                                                         "RTScanner"
                                                         };
    
    private List<NotifierEngine> engines;
    private MongoDatabase mongo;
    private Calendar lastNotificationTime;
    private Duration interval;
    
    
    
    /**
     * Identical to the above one, but sets the last dates hour and time to specify from which date and 
     * time it supposed to be sent.
     * @param engines
     * @param mongo
     * @param interval
     * @param notificationTime the time to notify next 
     */
    public TimedSummaryNotifier(List<NotifierEngine> engines, MongoDatabase mongo, Duration interval, Date notificationTime) {
        this.engines = engines;
        this.mongo = mongo;
        this.interval = interval;
        this.lastNotificationTime = new GregorianCalendar();
        this.lastNotificationTime.setTime(notificationTime);
    }
    /**
     * Constructor for when no specific time of day is wanted.
     * @param engines recipients
     * @param mongo the database to query
     * @param interval the interval between notifications
     */
    public TimedSummaryNotifier(List<NotifierEngine> engines, MongoDatabase mongo, Duration interval) {
        this(engines, mongo, interval, (new GregorianCalendar()).getTime());
    }
    

    protected void notifyEngines(String subject, String text) {
        for (NotifierEngine engine : this.engines) {
            engine.notify(subject, text);
        }
    }
    
    /**
     * Simple method for updating the calendar value
     */
    private void updateCalendar() {
        this.lastNotificationTime.setTime(new Date());
    }
    
    @Override
    public void run() {
        while(true) {
            if(Duration.between(lastNotificationTime.getTime().toInstant(), (new Date()).toInstant()).compareTo(interval) < 0) {
                createBsonFilter();
                String message = "Summary email from Repairnator. \n\n";
                message += "This summary contains the operations of Repairnator between " + lastNotificationTime.getTime().toString();
                updateCalendar();
                message += " and ";
                message += lastNotificationTime.getTime().toString() + ".\n";
                message += "Since the last summary Repairnator has: \n";
                
            }
            else {
                try {
                    Thread.sleep(TIME_TO_SLEEP);
                } catch (InterruptedException e) {
                    
                }
            }
        }
    }
    
    private Iterator<Document> queryDatabase(MongoCollection<Document> mongoCollection, Bson filter){
        FindIterable<Document> iterDoc = mongoCollection.find(filter);
        return iterDoc.iterator();
    }
    
    private Bson getBsonFilter() {
        Date previousDate = lastNotificationTime.getTime();
        return new Document("$gte", MONGO_DATE_FORMAT.format(previousDate));
    }
    private Bson getBsonFilter(String toolName) {
        Date previousDate = lastNotificationTime.getTime();
        
    }
}
