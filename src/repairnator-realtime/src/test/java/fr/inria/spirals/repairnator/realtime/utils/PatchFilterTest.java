package fr.inria.spirals.repairnator.realtime.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;
import org.kohsuke.github.*;

public class PatchFilterTest {
    
    String testRepoSlug = "javierron/continuous-sequencer-test";
    
    @Test
    public void testSingleLineCommit() throws IOException {
        
        PatchFilter filter = new PatchFilter();
        
        GitHub github = GitHub.connectAnonymously();
        GHRepository repo;
        GHCommit commit;
        
        repo = github.getRepository(testRepoSlug);
        commit = repo.getCommit("918b862a1e722a67337cf9f2a6485692efc23602");
        
        boolean filterMultiFile = true;
        boolean filterMultiHunk = true;
        int hunkDistance = 0;
        
        ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
        ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);
        
        assertEquals(1, hunks.size());
    }
    
    @Test
    public void testSingleLineCommitNonMatching() throws IOException {
        
        PatchFilter filter = new PatchFilter();
        
        GitHub github = GitHub.connectAnonymously();
        GHRepository repo;
        GHCommit commit;
        
        repo = github.getRepository(testRepoSlug);
        commit = repo.getCommit("14e4672ea8de7dbdc63b41b8ec9334c936ab515a");
        
        boolean filterMultiFile = true;
        boolean filterMultiHunk = true;
        int hunkDistance = 0;
        
        ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
        ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);
        
        assertEquals(0, hunks.size());
    }
    
    @Test
    public void testMultipleFilesSingleLineChanges() throws IOException {
        
        PatchFilter filter = new PatchFilter();
        
        GitHub github = GitHub.connectAnonymously();
        GHRepository repo;
        GHCommit commit;
        
        repo = github.getRepository(testRepoSlug);
        commit = repo.getCommit("14e4672ea8de7dbdc63b41b8ec9334c936ab515a");
        
        boolean filterMultiFile = false;
        boolean filterMultiHunk = true;
        int hunkDistance = 0;
        
        ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
        ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);
        
        assertEquals(3, hunks.size());
    }
    
    @Test
    public void testSingleFileMultiHunkChanges() throws IOException {
        
        PatchFilter filter = new PatchFilter();
        
        GitHub github = GitHub.connectAnonymously();
        GHRepository repo;
        GHCommit commit;
        
        repo = github.getRepository(testRepoSlug);
        commit = repo.getCommit("a3f4a35c980735e933a60f35eb7a2c243a28396c");
        
        boolean filterMultiFile = true;
        boolean filterMultiHunk = false;
        int hunkDistance = 0;
        
        ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
        ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);
        
        assertEquals(3, hunks.size());
    }
    
    @Test
    public void testMultipleFileMultiHunkChanges() throws IOException {
        
        PatchFilter filter = new PatchFilter();
        
        GitHub github = GitHub.connectAnonymously();
        GHRepository repo;
        GHCommit commit;
        
        repo = github.getRepository(testRepoSlug);
        commit = repo.getCommit("eabbfae4049ec34e04720c31d9c17d203834ec17");
        
        boolean filterMultiFile = false;
        boolean filterMultiHunk = false;
        int hunkDistance = 0;
        
        ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
        ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);
        
        assertEquals(10, hunks.size());
    }
    
    @Test
    public void testMultipleFileMultiHunkChangesNonMatching() throws IOException {
        
        PatchFilter filter = new PatchFilter();
        
        GitHub github = GitHub.connectAnonymously();
        GHRepository repo;
        GHCommit commit;
        
        repo = github.getRepository(testRepoSlug);
        commit = repo.getCommit("309fcf66423785546ec8c1d84853cb18f508ad0a");
        
        boolean filterMultiFile = false;
        boolean filterMultiHunk = false;
        int hunkDistance = 0;
        
        ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
        ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);
        
        assertEquals(0, hunks.size());
    }
}
