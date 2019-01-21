package fr.inria.spirals.repairnator.realtime.notifier;

import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Ignore;
import org.junit.Test;

import com.mongodb.client.model.Filters;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine; 

/**
 * TODO make usable with a way to utilise the config, and future create a test-database
 * @author benjamin
 *
 */

public class TestTimedSummaryNotifier {
    
    @Ignore
    public void testUpdateCalendar() {
        TimedSummaryNotifier notifier = new TimedSummaryNotifier(new ArrayList<NotifierEngine>(), Duration.ofHours(1), new Date());
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.HOUR, 1);
        
        notifier.updateCalendar(calendar.getTime());
        
        assertTrue(notifier.lastNotificationTime.getTime().compareTo(new Date()) < 0);
    }
    
    @Ignore
    public void testIntervalHasPassed() {
        TimedSummaryNotifier notifier = new TimedSummaryNotifier(new ArrayList<NotifierEngine>(), Duration.ofHours(1), new Date());
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.HOUR, -2);
        
        notifier.updateCalendar(calendar.getTime());
        
        assertTrue(notifier.intervalHasPassed());
    }
    
    @Ignore
    public void testUpdateFilters() {
        TimedSummaryNotifier notifier = new TimedSummaryNotifier(new ArrayList<NotifierEngine>(), Duration.ofHours(1), new Date());
        
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.HOUR, -2);
        
        notifier.updateFilters(calendar.getTime());
        Date previousDate = calendar.getTime();
        
        assertTrue(notifier.rtscannerFilter.equals(Filters.gte("dateWatched",
                TimedSummaryNotifier.MONGO_DATE_FORMAT.format(previousDate))));
        assertTrue(notifier.computeTestDirFilter.equals(Filters.and(
                Filters.gte("dateBuildEnd", TimedSummaryNotifier.MONGO_DATE_FORMAT.format(previousDate)),
                Filters.eq("realStatus", "/NopolAllTests/i"))));
        assertTrue(notifier.patchesFilter.equals(
                Filters.gte("date", TimedSummaryNotifier.MONGO_DATE_FORMAT.format(previousDate))));
    }
    
    
}
