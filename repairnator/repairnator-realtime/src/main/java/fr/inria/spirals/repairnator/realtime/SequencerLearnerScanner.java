package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.StateType;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

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
        JobHelperv2 jobHelperv2 = new JobHelperv2(RepairnatorConfig.getInstance().getJTravis());
        SequencerCollector collector = new SequencerCollector();
        
        final int scanBackIterations = 10;
        final int jumpSize = 250;
        
        while (true) {
            try {
                Optional<List<JobV2>> latestJobListOpt = jobHelperv2.allFromV2();
                List<JobV2> latestJobList = latestJobListOpt.get();
                int latestJobId = latestJobList.get(0).getId();
            	for (int it = 0; it < scanBackIterations; ++it) {
            		
            		List<JobV2> jobList = jobHelperv2.allSubSequentJobsFrom(latestJobId - (it*jumpSize));
            		for (JobV2 job : jobList) {
                    	if ("java".equals(job.getConfig().getLanguage()) && StateType.PASSED.equals(job.getState())) {
                        	collector.handle(job);                  
                        }
                    }
            	}
                

            } catch (Exception e) {
                e.printStackTrace();
            }
        } // end while loop
    }

}
