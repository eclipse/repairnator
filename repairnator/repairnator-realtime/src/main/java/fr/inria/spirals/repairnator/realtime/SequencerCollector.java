package fr.inria.spirals.repairnator.realtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.jtravis.entities.v2.JobV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.utils.PatchFilter;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.*;

/**
 * Filters and stores data for Sequencer training.
 */
public class SequencerCollector {

    private final String diffsPath = System.getProperty("user.home") + "/continuous-learning-data";
    private final int diffBatchSize = 100;
    
    private Git diffsRepo;

    private BuildHelperV2 buildHelper;
    private GitHub github;
    private PatchFilter filter;

    private boolean filterMultiFile;
    private boolean filterMultiHunk;
    private int hunkDistance;
    private Set<String> done;
    private int currentBatch;
    

    public SequencerCollector(boolean filterMultiFile, boolean filterMultiHunk, int hunkDistance) throws IOException {
        
        this.diffsRepo = Git.open(new File(diffsPath));
                
        this.filterMultiFile = filterMultiFile;
        this.filterMultiHunk = filterMultiHunk;
        this.hunkDistance = hunkDistance;
        this.done = new HashSet<>();

        buildHelper = new BuildHelperV2(RepairnatorConfig.getInstance().getJTravis());
        github = GitHub.connect(); // TODO: descriptive documentation about(or link to) .github file
    
        filter = new PatchFilter();
    }

    public SequencerCollector(boolean filterMultiFile, boolean filterMultiHunk) throws IOException {
        this(filterMultiFile, filterMultiHunk, 0);
    }

    public SequencerCollector() throws IOException {
        this(false, false, 0);
    }

    public void handle(JobV2 job) throws NoFilepatternException, GitAPIException {
        Optional<BuildV2> build = buildHelper.fromIdV2(job.getBuildId());
        if (!build.isPresent()) {
            return; // no diff - cannot find build.
        }
        String sha = build.get().getCommit().getSha();
        if (done.contains(sha)) {
            return;// already scanned commit
        }

        GHRepository repo;
        GHCommit commit;
        try {
            repo = github.getRepository(job.getRepositorySlug());
            commit = repo.getCommit(sha);

            ArrayList<String> patches = filter.getCommitPatches(commit, filterMultiFile, filterMultiHunk);
            ArrayList<String> hunks = filter.getHunks(patches, filterMultiHunk, hunkDistance);

            if (hunks.size() > 0) {
                saveFileDiff(job.getRepositorySlug(), sha);
                ++currentBatch;
                
                if(currentBatch >= diffBatchSize) { 
                    commitAndPushDiffs();
                    currentBatch = 0;
                }
            }

            done.add(sha);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;// "error";
        }

    }

    void initDiffsRepo() {
        
    }

    void commitAndPushDiffs() throws NoFilepatternException, GitAPIException {
        diffsRepo.add().addFilepattern(".").call();
        diffsRepo.commit().setMessage("diff files").call();
        
        String OAUTH_TOKEN = System.getenv("OAUTH_TOKEN"); //TODO: read from environment.
        
        RefSpec spec = new RefSpec("master:master");
        diffsRepo.push()
            .setRemote("origin")
            .setRefSpecs(spec)
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(OAUTH_TOKEN, ""))
            .call();
    }

    void saveFileDiff(String slug, String sha) throws IOException {
        System.out.println("saving file...");
        URL url = new URL("https", "github.com", "/" + slug + "/commit/" + sha + ".diff");
        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());

        File f = new File(diffsPath + "/" + slug.replace("/", "-") + "-" + sha + ".diff");
        f.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(f, false);

        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        
        fileOutputStream.close();
    }
}
