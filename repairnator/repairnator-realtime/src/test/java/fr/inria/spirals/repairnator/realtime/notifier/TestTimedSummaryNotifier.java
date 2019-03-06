package fr.inria.spirals.repairnator.realtime.notifier;

import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import org.bson.BsonDocument;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.client.model.Filters;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine; 

/**
 * A test class for the basic functions of the TimedSummaryNotifier.
 * Tests that the calendar updates properly, that it correctly recognises whether the time has passed,
 * whether it can update the mongodb filters properly and lastly that the message is correctly created.
 * @author benjamin
 *
 */

public class TestTimedSummaryNotifier {
    
    private static final String[] tools = {"Tool0", "Tool1", "Tool2", "Tool3", "Tool4"};
    private TimedSummaryNotifier notifier;
    
    @Before
    public void setUp() {
        this.notifier = new TimedSummaryNotifier(
                new ArrayList<NotifierEngine>(),
                Duration.ofHours(1),
                tools,
                "host",
                "dbName",
                new Date());
    }
    
    /**
     * Test that the calendar is correctly updated
     */
    @Test
    public void testUpdateLastNotificationTime() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.HOUR, -1);
        
        notifier.updateLastNotificationTime(calendar.getTime());
        
        assertTrue(notifier.lastNotificationTime.getTime().compareTo(new Date()) < 0);
    }
    
    /**
     * Tests that the notifier can see whether an interval has passed
     */
    @Test
    public void testIntervalHasPassed() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.HOUR, -2);
        
        notifier.updateLastNotificationTime(calendar.getTime());
        
        assertTrue(notifier.intervalHasPassed());
    }
    
    /**
     * Tests that the filters are properly updated when the calendar is changed
     */
    @Test
    public void testUpdateFilters() {
        
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.HOUR, -2);
        
        notifier.updateFilters(calendar.getTime());
        Date previousDate = calendar.getTime();
        
        BsonDocument rtscannerFilter = notifier.rtscannerFilter.toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry());
        BsonDocument computeTestDirFilter = notifier.computeTestDirFilter.toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry());
        BsonDocument patchesFilter = notifier.patchesFilter.toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry());
        
        BsonDocument[] toolFilters = new BsonDocument[tools.length];
        for(int i = 0; i < tools.length; i++) {
            toolFilters[i] = notifier.toolFilters.get(tools[i]).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry());
        }
        
        
        assertTrue(rtscannerFilter.equals(Filters.gte("dateWatched",
                TimedSummaryNotifier.MONGO_DATE_FORMAT.format(previousDate)).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry())));
        assertTrue(computeTestDirFilter.equals(Filters.and(
                Filters.gte("dateBuildEnd", TimedSummaryNotifier.MONGO_DATE_FORMAT.format(previousDate)),
                Filters.eq("realStatus", "/NopolAllTests/i")).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry())));
        assertTrue(patchesFilter.equals(
                Filters.gte("date", TimedSummaryNotifier.MONGO_DATE_FORMAT.format(previousDate)).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry())));
        for(int i = 0; i < toolFilters.length; i++) {
            assertTrue(toolFilters[i].equals(
                    Filters.and(Filters.gte("date", TimedSummaryNotifier.MONGO_DATE_FORMAT.format(previousDate)),
                            Filters.eq("toolname", tools[i])).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry())));
        }
    }
    
    /**
     * Tests that the message is correctly composed.
     */
    @Test
    public void testCreateMessage() {
        int[] patchesPerTool = {1, 5, 10, 15, 20};
        Date now = new Date();
        String message = notifier.createMessage(100, 15, 51, patchesPerTool, now);
        String testMessage = "Summary email from Repairnator. \n\n"
                + "This summary contains the operations of Repairnator between "
                + notifier.lastNotificationTime.getTime().toString()
                + " and "
                + now.toString() + ".\n"
                + "Since the last summary Repairnator has: \n\n"
                + "Number of analyzed builds: 100\n"
                + "Number of repair attempts made: 15\n"
                + "Total number of patches: 51\n"
                + "Total number of patches found by Tool0: 1\n"
                + "Total number of patches found by Tool1: 5\n"
                + "Total number of patches found by Tool2: 10\n"
                + "Total number of patches found by Tool3: 15\n"
                + "Total number of patches found by Tool4: 20\n";
        assertTrue(message.equals(testMessage));
    }
}
