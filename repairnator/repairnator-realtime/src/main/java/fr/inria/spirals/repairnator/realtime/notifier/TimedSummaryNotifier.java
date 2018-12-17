package fr.inria.spirals.repairnator.realtime.notifier;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document; 

public class TimedSummaryNotifier implements Runnable{
    private List<NotifierEngine> engines;
    private MongoDatabase mongo;
    private Calendar lastNotificationTime;
    private Duration interval;
    private 
    
    
    
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
                
                String message = "Summary email from Repairnator. \n\n";
                message += "The last summary was sent on " + lastNotificationTime.getTime().toString() " and it is now ";
                updateCalendar();
                message += lastNotificationTime.getTime().toString() + ".\n";
                message += "Since the last summary Repairnator has: \n";
                
            }
        }
    }
    
    private Iterator<Document> queryDatabase(MongoCollection<Document> mongoCollection){
        
    }
    
    private void createBsonFilter() {
        
    }
}
