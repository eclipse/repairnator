package fr.inria.spirals.repairnator.process.utils4tests;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;

import java.io.File;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectInspectorMocker {

    public static ProjectInspector mockProjectInspector(JobStatus jobStatus) {
        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getJobStatus()).thenReturn(jobStatus);
        return inspector;
    }

    public static ProjectInspector mockProjectInspector(JobStatus jobStatus, String localRepoPath) {
        ProjectInspector inspector = mockProjectInspector(jobStatus);
        when(inspector.getRepoLocalPath()).thenReturn(localRepoPath);
        return inspector;
    }

    public static ProjectInspector mockProjectInspector(JobStatus jobStatus, File tmpDir, BuildToBeInspected buildToBeInspected) {
        ProjectInspector inspector = mockProjectInspector(jobStatus);
        when(inspector.getRepoSlug()).thenReturn(buildToBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getRepoToPushLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repotopush");
        when(inspector.getBuildToBeInspected()).thenReturn(buildToBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(buildToBeInspected.getBuggyBuild());
        when(inspector.getPatchedBuild()).thenReturn(buildToBeInspected.getPatchedBuild());
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());
        return inspector;
    }

    public static ProjectInspector mockProjectInspector(JobStatus jobStatus, File tmpDir, BuildToBeInspected buildToBeInspected, CheckoutType checkoutType) {
        ProjectInspector inspector = mockProjectInspector(jobStatus, tmpDir, buildToBeInspected);
        when(inspector.getCheckoutType()).thenReturn(checkoutType);
        return inspector;
    }

}
