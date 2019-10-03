package fr.inria.spirals.repairnator.realtime;


import java.io.IOException;
import java.util.Optional;
import java.util.Vector;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.client.*;

import fr.inria.jtravis.helpers.BuildHelper;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

public class SequencerCollector {
	
	private Vector<String> data;
	private BuildHelper buildHelper;
	private GitHubClient gitHubClient;
	
	public SequencerCollector() {
		buildHelper = RepairnatorConfig.getInstance().getJTravis().build();
		data = new Vector<String>();
	}
	
	public void add(JobV2 job) {
		data.add(getDiff(job));
	}
	
	private String getDiff(JobV2 job) {
		System.out.println(job.getBuildId());
		Optional<Build> build = buildHelper.fromId(job.getBuildId());
		if(!build.isPresent()) {
			return "no diff"; //TODO: handle this properly.
		}
		String sha = build.get().getCommit().getSha();
		
		gitHubClient = new GitHubClient();
		
		GitHubRequest req = new GitHubRequest();
		req.setUri("/repos/" + job.getRepositorySlug() + "/commits/" + sha);
		
		GitHubResponse resp;
		
		 try {
			resp = gitHubClient.get(req);
		} catch (IOException e) {
			return "no diff - error"; //TODO: handle this properly
		}
		
		Commit commit = (Commit)resp.getBody();
		
		return "the diff";
	}
	
}
