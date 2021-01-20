package fr.inria.spirals.repairnator.process.step;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.states.PipelineState;

public class CloneCheckoutBranchRepository extends AbstractStep {


	public CloneCheckoutBranchRepository(ProjectInspector inspector) {
        super(inspector, true);
    }
	
	@Override
    protected StepStatus businessExecute() {

		GitRepositoryProjectInspector githubInspector;

		try {
			githubInspector = (GitRepositoryProjectInspector) getInspector();
		}catch (Exception ex){
			this.addStepError("CloneCheckoutBranchRepository only supports GitHub commit references");
			return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
		}
        
        String repoUrl = githubInspector.getGitRepositoryUrl() + ".git";
        String branch = null;
        if (githubInspector.getGitRepositoryBranch() != null) {
        	branch = "refs/heads/" + githubInspector.getGitRepositoryBranch();
        }
        
        String repoLocalPath = githubInspector.getRepoLocalPath();
        try {
            this.getLogger().info("Cloning repository " + repoUrl + " in the following directory: " + repoLocalPath);

            List<String> branchList = new ArrayList<String>();
            branchList.add(branch);
            
            CloneCommand cloneRepositoryCommand = Git.cloneRepository()
            		.setCloneSubmodules(true)
            		.setURI( repoUrl )
            		.setDirectory(new File(repoLocalPath));
            if (branch != null) {
            	cloneRepositoryCommand.setBranchesToClone(branchList).setBranch(branch);
            }
            
            Git git = cloneRepositoryCommand.call();
            Repository repository = git.getRepository();
            
            List<RevCommit> commits = getBranchCommits(repository, repoLocalPath);
            
            if (getConfig().isGitRepositoryFirstCommit()) {
            	git.checkout().setName(commits.get(commits.size()-1).getName()).call();
            } else if (getConfig().getGitRepositoryIdCommit() != null) {
            	git.checkout().setName(getConfig().getGitRepositoryIdCommit()).call();
            } else {
            	git.checkout().setName(commits.get(0).getName()).call();
            }
            
            return StepStatus.buildSuccess(this);
        } catch (Exception e) {
            this.addStepError("Repository " + repoUrl + " cannot be cloned.", e);
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }
    }
	
	private List<RevCommit> getBranchCommits(Repository repository, String path) {
    	
		String treeName = null;
		try {
			treeName = repository.getBranch();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		List<RevCommit> commitList = new ArrayList<>();
		
		try {
			Git git = Git.open(new File(path));
			git.log().add(repository.resolve(treeName)).call().forEach(commitList::add);
		} catch (RevisionSyntaxException | GitAPIException | IOException e) {
			e.printStackTrace();
		}

		return commitList;
	}
}
