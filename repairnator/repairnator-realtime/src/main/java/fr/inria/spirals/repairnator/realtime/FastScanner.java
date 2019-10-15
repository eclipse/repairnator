package fr.inria.spirals.repairnator.realtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.StateType;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.jtravis.helpers.JobHelper;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Super fast scanner inspired from @tdurieux's travis-listener https://github.com/tdurieux/travis-listener
 * The core idea is to directly iterate over build and job numbers. 
 */
public class FastScanner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastScanner.class);

    private RTScanner rtScanner;
    public int nMaxBuildsToBeAnalyzedWithPipeline = 100;

    public FastScanner() {
        this.rtScanner = new RTScanner("foo");
    }

    public static void main(String[] args) {
        new FastScanner().run();
    }

    @Override
    public void run() {
        LOGGER.debug("Start running inspect Jobs...");
        int nInteresting = 0;
        Set<Long> done = new HashSet<>();
        JobHelperv2 jobHelperv2 = new JobHelperv2(RepairnatorConfig.getInstance().getJTravis());
        SequencerCollector collector = new SequencerCollector();
        while (true) {
            try {
                Optional<List<JobV2>> jobListOpt = jobHelperv2.allFromV2();
                List<JobV2> jobList = jobListOpt.get();


                Map<StateType, Integer> stats = new HashMap<>();
                int nstats=0;
                int N=20;
                int GO_IN_THE_PAST= 100;
                for (int k=0;k<N;k++) {
                    for (JobV2 job : jobHelperv2.allSubSequentJobsFrom(jobList.get(0).getId() - 250*N - GO_IN_THE_PAST)) {
                        if (stats.keySet().contains(job.getState())) {
                            stats.put(job.getState(), stats.get(job.getState()) + 1);
                        } else {
                            stats.put(job.getState(), 1);
                        }
                        nstats++;

                        if ("java".equals(job.getConfig().getLanguage()) && StateType.FAILED.equals(job.getState()) && ! done.contains(job.getBuildId())) {
                            System.out.println("=====" + job.getId() + " " +job.getBuildId());
                            done.add((long)job.getBuildId());

//                            Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(job.getBuildId());
//                            FastScanner.this.rtScanner.submitBuildToExecution(optionalBuild.get());
                        } else if ("java".equals(job.getConfig().getLanguage()) && StateType.PASSED.equals(job.getState()) && ! done.contains(job.getBuildId())) {
                        	System.out.println("==Job id " +  job.getId() + "====Job Commit ID -> " + job.getCommitId()  + "=== Slug: " + job.getRepositorySlug());
                        	collector.add(job);                  
                        }
                    }
                }

                System.out.println(stats);
                System.out.println(stats.get(StateType.FAILED)*1./nstats);

                if (done.size()>nMaxBuildsToBeAnalyzedWithPipeline) {
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } // end while loop
    }

}
