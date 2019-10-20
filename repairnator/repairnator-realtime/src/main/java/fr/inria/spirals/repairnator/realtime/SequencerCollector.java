package fr.inria.spirals.repairnator.realtime;


import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Vector;


import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import org.kohsuke.github.*;


/**
 * Filters and stores data for Sequencer training.
 * */
public class SequencerCollector {
	
	private Vector<String> data;
	private BuildHelperV2 buildHelper;
	GitHub github;
	
	public SequencerCollector() {
		buildHelper = new BuildHelperV2(RepairnatorConfig.getInstance().getJTravis());
		data = new Vector<String>();
		try {
			github = GitHub.connect(); //TODO: descriptive documentation about(or link to) .github file
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void handle(JobV2 job) {
		String diff = getDiff(job);
		data.add(diff);
		
		//System.out.println(diff);
	}
	
	private String getDiff(JobV2 job) {
		Optional<BuildV2> build = buildHelper.fromIdV2(job.getBuildId());
		if(!build.isPresent()) {
			return "no diff - cannot find build"; //TODO: handle this properly.
		}
		String sha = build.get().getCommit().getSha();
	
		GHRepository repo;
		GHCommit commit;
		try {
			repo = github.getRepository(job.getRepositorySlug());
			commit = repo.getCommit(sha);
			
			List<GHCommit.File> files = commit.getFiles(); 
			
			if(files.size() == 1 && files.get(0).getFileName().endsWith(".java")) {
				System.out.println("Patch: " + files.get(0).getPatch() );
				return "single-file diff";	
			}
			
			return "multi-file diff";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "error";
		}
		
	}
	
}
