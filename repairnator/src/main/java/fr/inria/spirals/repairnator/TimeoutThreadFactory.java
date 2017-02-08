package fr.inria.spirals.repairnator;

import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by urli on 07/02/2017.
 */
public class TimeoutThreadFactory implements ThreadFactory {
    private ThreadFactory defaultThreadFactory;
    private final long duration;
    private final TimeUnit unit;
    private final Logger logger;

    public TimeoutThreadFactory(long duration, TimeUnit unit, Logger logger) {
        this.duration = duration;
        this.unit = unit;

        this.defaultThreadFactory = Executors.defaultThreadFactory();
        this.logger = logger;
    }


    @Override
    public Thread newThread(Runnable r) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ExecutorService singlePool = Executors.newSingleThreadExecutor();
                Future runningThread = singlePool.submit(r, true);

                try {
                    singlePool.shutdown();
                    runningThread.get(duration, unit);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    logger.warn("Following exception occured: "+e.toString());
                    runningThread.cancel(true);
                    singlePool.shutdownNow();
                }
            }
        };

        return this.defaultThreadFactory.newThread(runnable);
    }
}
