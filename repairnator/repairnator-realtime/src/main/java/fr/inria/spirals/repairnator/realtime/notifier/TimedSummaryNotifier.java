package fr.inria.spirals.repairnator.realtime.notifier;

import java.util.List;

import fr.inria.spirals.repairnator.notifier.engines.NotifierEngine;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;

public class TimedSummaryNotifier implements Runnable{
    private List<NotifierEngine> engines;
    private MongoConnection mongo;
    
    public TimedSummaryNotifier(List<NotifierEngine> engines, MongoConnection mongo) {
        this.engines = engines;
        this.mongo = mongo;
    }

    protected void notifyEngines(String subject, String text) {
        for (NotifierEngine engine : this.engines) {
            engine.notify(subject, text);
        }
    }
    
    @Override
    public void run() {
        
    }

}
