package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.StateType;
import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Scanner based on FastScanner.java but re-purposed for scanning for Sequencer training data.
 */
public class SequencerLearnerScanner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SequencerLearnerScanner.class);
    private BuildHelperV2 buildHelper;
    
    public static void main(String[] args) {
        new SequencerLearnerScanner().run();
        
    }
    
    public SequencerLearnerScanner() {
        buildHelper = new BuildHelperV2(RepairnatorConfig.getInstance().getJTravis());
    }

    @Override
    public void run() {
        LOGGER.debug("Start running inspect Jobs...");
        JobHelperv2 jobHelperv2 = new JobHelperv2(RepairnatorConfig.getInstance().getJTravis());
        SequencerCollector collector = null;
        
        try {
            collector = new SequencerCollector();
            collector.initialize();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
        
        final int scanBackIterations = 10;
        final int jumpSize = 250;
        
        Set<Integer> done = new HashSet<>();
        
        while (true) {
            try {
                Optional<List<JobV2>> latestJobListOpt = jobHelperv2.allFromV2();
                List<JobV2> latestJobList = latestJobListOpt.get();
                int latestJobId = latestJobList.get(0).getId();
            	for (int it = 0; it < scanBackIterations; ++it) {
            		
            		List<JobV2> jobList = jobHelperv2.allSubSequentJobsFrom(latestJobId - (it*jumpSize));
            		for (JobV2 job : jobList) {
                    	if (!done.contains(job.getBuildId()) && "java".equals(job.getConfig().getLanguage()) && StateType.PASSED.equals(job.getState())) {
                        	
                    	    Optional<BuildV2> build = buildHelper.fromIdV2(job.getBuildId());
                    	    
                    	    if (!build.isPresent()) {
                                continue;
                            }
                    	    
                    	    String sha = build.get().getCommit().getSha();
                    	    
                    	    collector.handle(job.getRepositorySlug(), sha);   
                        	done.add(job.getBuildId());
                        }
                    }
            	}
                

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } // end while loop
    }

}
