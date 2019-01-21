package fr.inria.spirals.repairnator.realtime.notifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine; 

public class TestTimedSummaryNotifier {
    
    
    
    @Test
    public void testUpdateCalendar() {
        TimedSummaryNotifier notifier = new TimedSummaryNotifier(new ArrayList<NotifierEngine>(), Duration.ofHours(1), new Date());
        
    }
}
