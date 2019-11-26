package fr.inria.spirals.repairnator.realtime;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.utils.PatchFilter;
import fr.inria.spirals.repairnator.realtime.utils.PatchFilter.HunkLines;

import org.kohsuke.github.*;


/**
 * Filters and stores data for Sequencer training.
 * */
public class SequencerCollector {
    
    //TODO: find a temporary storage solution.
    private final String addedFilename = System.getProperty("user.home") + "/added.txt";
    private final String removedFilename = System.getProperty("user.home") + "/removed.txt";

	private BuildHelperV2 buildHelper;
	private GitHub github;
	private PatchFilter filter;
	
	private boolean filterMultiFile;
	private boolean filterMultiHunk;
	private int hunkDistance;
	private Set<String> done;
	
	public SequencerCollector(boolean filterMultiFile, boolean filterMultiHunk, int hunkDistance) {
	    this.filterMultiFile = filterMultiFile;
        this.filterMultiHunk = filterMultiHunk;
        this.hunkDistance = hunkDistance;
        this.done = new HashSet<>();
        
        buildHelper = new BuildHelperV2(RepairnatorConfig.getInstance().getJTravis());
        try {
            github = GitHub.connect(); //TODO: descriptive documentation about(or link to) .github file
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        filter = new PatchFilter();
	}

	public SequencerCollector(boolean filterMultiFile, boolean filterMultiHunk) {
        this(filterMultiFile, filterMultiHunk, 0);
    }
	
	public SequencerCollector() {
        this(false, false, 0);
    }
	
    
	public void handle(JobV2 job) {
		Optional<BuildV2> build = buildHelper.fromIdV2(job.getBuildId());
		if(!build.isPresent()) {
			return; // no diff - cannot find build.
		}
		String sha = build.get().getCommit().getSha();
		if(done.contains(sha)) {
		    return;// already scanned commit
		}
	
		GHRepository repo;
		GHCommit commit;
		try {
			repo = github.getRepository(job.getRepositorySlug());
			commit = repo.getCommit(sha);
			
			ArrayList<String> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
			ArrayList<String> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);

			FileWriter addedFileWriter = new FileWriter(addedFilename, true);
		    PrintWriter addedPrintWriter = new PrintWriter(addedFileWriter, true);
		    FileWriter removedFileWriter = new FileWriter(removedFilename, true);
		    PrintWriter removedPrintWriter = new PrintWriter(removedFileWriter, true);
			
			for(String hunk : hunks) {
        		HunkLines lines = filter.parse(hunk);
			    
        		addedPrintWriter.println(lines.added);
        		removedPrintWriter.println(lines.removed);
        		
        		System.out.println("===============");
			    System.out.println(hunk);
			}
			
			
			removedPrintWriter.close();
			addedFileWriter.close();
			
			done.add(sha);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;// "error";
		}
		
	}
}
