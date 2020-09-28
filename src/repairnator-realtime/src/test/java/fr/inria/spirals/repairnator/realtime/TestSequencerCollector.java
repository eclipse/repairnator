package fr.inria.spirals.repairnator.realtime;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import fr.inria.spirals.repairnator.realtime.utils.PatchFilter;
import fr.inria.spirals.repairnator.realtime.utils.SequencerCollectorHunk;
import fr.inria.spirals.repairnator.realtime.utils.SequencerCollectorPatch;

@Ignore
public class TestSequencerCollector {
    
    @Mock
    GitHub github;
    @Mock
    GHCommit mockCommit;
    @Mock
    GHRepository mockRepo;
    @Mock
    PatchFilter filter;
    
    @Spy
    @InjectMocks
    SequencerCollector collector;

    
    @Before public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testDiffSaveAndPush() throws IOException{
        
        ArrayList<SequencerCollectorPatch> emptyList = new ArrayList<>();
        ArrayList<SequencerCollectorHunk> mockHunkList = new ArrayList<>(); mockHunkList.add(
                new SequencerCollectorHunk(1, "file.java", "hunk1")); 

        //Mock external calls
        Mockito.when(github.getRepository(Mockito.anyString())).thenReturn(mockRepo);
        Mockito.when(mockRepo.getCommit(Mockito.anyString())).thenReturn(mockCommit);
        
        
        //Mock hunk filter since mock commit is used
        Mockito.when(filter.getCommitPatches(Mockito.any(GHCommit.class), Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(emptyList);
        Mockito.when(filter.getHunks(Mockito.any(ArrayList.class), Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(mockHunkList);
        
        //Mock save/commit/push methods
        Mockito.doNothing().when(collector).saveFileDiff(Mockito.anyString(), Mockito.anyString());
    }
    
}
