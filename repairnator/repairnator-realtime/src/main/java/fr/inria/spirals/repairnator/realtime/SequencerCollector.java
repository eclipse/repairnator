package fr.inria.spirals.repairnator.realtime;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.utils.PatchFilter;
import fr.inria.spirals.repairnator.realtime.utils.PatchFilter.PatchLines;

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
	
	public SequencerCollector() {
		buildHelper = new BuildHelperV2(RepairnatorConfig.getInstance().getJTravis());
		try {
			github = GitHub.connect(); //TODO: descriptive documentation about(or link to) .github file
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		filter = new PatchFilter();
	}
	
	
	public void handle(JobV2 job) {
		Optional<BuildV2> build = buildHelper.fromIdV2(job.getBuildId());
		if(!build.isPresent()) {
			return; // no diff - cannot find build.
		}
		String sha = build.get().getCommit().getSha();
	
		GHRepository repo;
		GHCommit commit;
		try {
			repo = github.getRepository(job.getRepositorySlug());
			commit = repo.getCommit(sha);
			
			List<GHCommit.File> files = commit.getFiles(); 
			
			if(files.size() != 1 || !files.get(0).getFileName().endsWith(".java")) {
			    return;// multi-file diff
			}
			
			String patch = files.get(0).getPatch();
				
			if (!filter.test(patch)) {
			    return; // multi-hunk or multi-line patch	   
			}
			
			System.out.println("Found:");
			System.out.println(patch);
			
			PatchLines lines = filter.parse(patch);
			
			FileWriter addedFileWriter = new FileWriter(addedFilename, true);
		    PrintWriter addedPrintWriter = new PrintWriter(addedFileWriter, true);
		    
		    addedPrintWriter.println(lines.added);
		    addedPrintWriter.close();
		    
		    FileWriter removedFileWriter = new FileWriter(removedFilename, true);
            PrintWriter removedPrintWriter = new PrintWriter(removedFileWriter, true);
            
            removedPrintWriter.println(lines.removed);
            removedPrintWriter.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;// "error";
		}
		
	}
	
}
