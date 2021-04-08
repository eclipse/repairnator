package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.config.SequencerConfig;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Scanner based on FastScanner.java but re-purposed for scanning for Sequencer training data.
 */
public class SequencerLearnerScanner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SequencerLearnerScanner.class);

    public static void main(String[] args) {
        new SequencerLearnerScanner().run();
    }
    
    @Override
    public void run() {
        LOGGER.debug("Start running inspect Jobs...");

        GithubScanner scanner = new GithubScanner(GithubScanner.FetchMode.PASSING);
        SequencerCollector collector;
        
        try {
            collector = new SequencerCollector(SequencerConfig.getInstance().contextSize);
            collector.initialize();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        
        Set<String> done = new HashSet<>();
        while (true) {
            try {
                List<SelectedCommit> latestJobList = scanner.fetch(); // default fetch?
            		for (SelectedCommit job : latestJobList) {
                    	    collector.handle(job.getRepoName(), job.getCommitId());
                        	done.add(job.getRepoName() + job.getCommitId());
                    }
                TimeUnit.MILLISECONDS.sleep(60 * 60 * 1000); // 1 hour
            } catch (InterruptedException e){
                System.err.println("Wait period interrupted.");
                throw new RuntimeException(e);
            } catch (Exception e) {
                System.err.println(e.toString());
                System.err.println("failed to get commit");
                //throw new RuntimeException(e);
            }

        } // end while loop
    }

}
