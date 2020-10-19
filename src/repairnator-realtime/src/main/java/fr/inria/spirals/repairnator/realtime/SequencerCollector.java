package fr.inria.spirals.repairnator.realtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private GitHub github;
    private PatchFilter filter;

    private boolean filterMultiFile;
    private boolean filterMultiHunk;
    private int hunkDistance;
    private Set<String> done;

    private int contextSize;


    public SequencerCollector(int contextSize, boolean filterMultiFile, boolean filterMultiHunk, int hunkDistance) {
        
        this.filterMultiFile = filterMultiFile;
        this.filterMultiHunk = filterMultiHunk;
        this.hunkDistance = hunkDistance;
        this.done = new HashSet<>();
        this.contextSize = contextSize;

        filter = new PatchFilter();
    }

    public SequencerCollector(int contextSize, boolean filterMultiFile, boolean filterMultiHunk) {
        this(contextSize, filterMultiFile, filterMultiHunk, 0);
    }

    public SequencerCollector(int contextSize) {
        this(contextSize, false, false, 0);
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

            Map<String, String> allRawFiles = new HashMap<>();

            ArrayList<SequencerCollectorPatch> patches = filter.getCommitPatches(commit, filterMultiFile, contextSize, allRawFiles);
            ArrayList<SequencerCollectorHunk> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);

            if (hunks.size() > 0) {
                
                //create directory for file

                String dirPath = diffsPath + "/" + repositorySlug.replace("/", "-") + "-" + sha;
                File f = new File(dirPath);
                f.mkdir();

                // save hunks
                hunks.forEach( (hunk) -> {
                    try {
                        String filename = hunk.getFile();
                        BufferedWriter writer;
                        writer = new BufferedWriter(
                                new FileWriter(dirPath + "/" + filename.substring(filename.lastIndexOf("/") + 1) + "-" + hunk.getLine()));
                        writer.append(Integer.toUnsignedLong(filename.hashCode()) + "\n");
                        writer.append(hunk.getContent());
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });


                // save parent files
                Set<String> neededFiles = hunks.stream().map(SequencerCollectorHunk::getFile).collect(Collectors.toSet());
                Map<String, String> purgedRawFiles = neededFiles.stream()
                        .collect(Collectors.toMap(Function.identity(), allRawFiles::get));

                purgedRawFiles.forEach((file, content) -> {
                    try {

                        BufferedWriter writer;
                        writer = new BufferedWriter(
                                new FileWriter(dirPath + "/" + Integer.toUnsignedLong(file.hashCode())));
                        writer.append(content);
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                // save diff
                saveFileDiff(repositorySlug, sha);

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
