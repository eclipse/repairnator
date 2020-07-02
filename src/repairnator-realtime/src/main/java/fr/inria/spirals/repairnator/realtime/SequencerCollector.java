package fr.inria.spirals.repairnator.realtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import fr.inria.spirals.repairnator.config.SequencerConfig;
import fr.inria.spirals.repairnator.realtime.utils.PatchFilter;
import fr.inria.spirals.repairnator.realtime.utils.SequencerCollectorHunk;
import fr.inria.spirals.repairnator.realtime.utils.SequencerCollectorPatch;

import org.kohsuke.github.*;

/**
 * Filters and stores data for Sequencer training.
 */
public class SequencerCollector {
    
    private final String diffsPath = SequencerConfig.getInstance().collectorPath;
    
    private final int diffBatchSize = 100;
    
    private GitHub github;
    private PatchFilter filter;

    private boolean filterMultiFile;
    private boolean filterMultiHunk;
    private int hunkDistance;
    private Set<String> done;
    private int currentBatch;
    

    public SequencerCollector(boolean filterMultiFile, boolean filterMultiHunk, int hunkDistance) {
        
        this.filterMultiFile = filterMultiFile;
        this.filterMultiHunk = filterMultiHunk;
        this.hunkDistance = hunkDistance;
        this.done = new HashSet<>();

        filter = new PatchFilter();
    }

    public SequencerCollector(boolean filterMultiFile, boolean filterMultiHunk) {
        this(filterMultiFile, filterMultiHunk, 0);
    }

    public SequencerCollector() {
        this(false, false, 0);
    }

    public void handle(String repositorySlug, String sha) {
        
        if (done.contains(sha)) {
            return;
        }
        
        GHRepository repo;
        GHCommit commit;
                
        try {
            repo = github.getRepository(repositorySlug);
            commit = repo.getCommit(sha);

            ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
            ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);

            if (hunks.size() > 0) {
                
                //create directory for file
                
                String dirPath = diffsPath + "/" + repositorySlug.replace("/", "-") + "-" + sha;
                File f = new File(dirPath);
                f.mkdir();
                
                hunks.forEach( (hunk) -> {
                    try {
                        BufferedWriter writer;
                            writer = new BufferedWriter(new FileWriter(dirPath + "/" + hunk.getFile() + "-" + hunk.getLine()));
                        writer.append(hunk.getContent()); 
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                
                saveFileDiff(repositorySlug, sha);
                ++currentBatch;
                
                if(currentBatch >= diffBatchSize) {
                    currentBatch = 0;
                }
            }

            done.add(sha);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    
    public void initialize() throws IOException {
        initGithubConnection();
    }

    
    void initGithubConnection() throws IOException {
        this.github = GitHub.connect(); // read credentials from ~/.github file
    }

    protected void saveFileDiff(String slug, String sha) throws IOException {
        System.out.println("saving file...");
        URL url = new URL("https", "github.com", "/" + slug + "/commit/" + sha + ".diff");
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());

        File f = new File(diffsPath + "/" + slug.replace("/", "-") + "-" + sha + "/commit.diff");
        f.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(f, false);

        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        
        fileOutputStream.close();
    }
}
