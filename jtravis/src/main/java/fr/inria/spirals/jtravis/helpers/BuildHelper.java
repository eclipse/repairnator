package fr.inria.spirals.jtravis.helpers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Config;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Repository;
import okhttp3.ResponseBody;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * The helper to deal with Build objects
 *
 * @author Simon Urli
 */
public class BuildHelper extends AbstractHelper {

    public static final String BUILD_ENDPOINT = "builds/";

    private static BuildHelper instance;

    private BuildHelper() {
        super();
    }

    private static BuildHelper getInstance() {
        if (instance == null) {
            instance = new BuildHelper();
        }
        return instance;
    }

    public static Build getBuildFromId(int id, Repository parentRepo) {
        String resourceUrl = TRAVIS_API_ENDPOINT+BUILD_ENDPOINT+id;

        try {
            String response = getInstance().get(resourceUrl);
            JsonParser parser = new JsonParser();
            JsonObject allAnswer = parser.parse(response).getAsJsonObject();

            JsonObject buildJSON = allAnswer.getAsJsonObject("build");
            Build build = createGson().fromJson(buildJSON, Build.class);

            JsonObject commitJSON = allAnswer.getAsJsonObject("commit");
            Commit commit = CommitHelper.getCommitFromJsonElement(commitJSON);
            build.setCommit(commit);

            if (parentRepo != null) {
                build.setRepository(parentRepo);
            }

            JsonObject configJSON = buildJSON.getAsJsonObject("config");
            Config config = ConfigHelper.getConfigFromJsonElement(configJSON);
            build.setConfig(config);

            JsonArray arrayJobs = allAnswer.getAsJsonArray("jobs");

            for (JsonElement jobJSONElement : arrayJobs) {
                Job job = JobHelper.createJobFromJsonElement((JsonObject)jobJSONElement);
                build.addJob(job);
            }

            if (build.isPullRequest()) {
                GitHub github = getInstance().getGithub();
                GHRateLimit rateLimit = github.getRateLimit();
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                AbstractHelper.LOGGER.debug("GitHub ratelimit: Limit: "+rateLimit.limit+" Remaining: "+rateLimit.remaining+" Reset hour: "+dateFormat.format(rateLimit.reset));

                if (rateLimit.remaining > 2) {
                    GHRepository ghRepo = github.getRepository(build.getRepository().getSlug());
                    GHPullRequest pullRequest = ghRepo.getPullRequest(build.getPullRequestNumber());


                    GHCommitPointer base = pullRequest.getBase();
                    GHRepository headRepo = pullRequest.getHead().getRepository();

                    Repository repoPR = new Repository();
                    repoPR.setId(headRepo.getId());
                    repoPR.setDescription(headRepo.getDescription());
                    repoPR.setActive(true);
                    repoPR.setSlug(headRepo.getFullName());

                    build.setPRRepository(repoPR);

                    Commit commitHead = new Commit();
                    commitHead.setSha(base.getSha());
                    commitHead.setBranch(base.getRef());
                    commitHead.setCompareUrl(base.getCommit().getHtmlUrl().toString());

                    GHCommit.ShortInfo infoCommit = base.getCommit().getCommitShortInfo();

                    commitHead.setMessage(infoCommit.getMessage());
                    commitHead.setCommitterEmail(infoCommit.getAuthor().getEmail());
                    commitHead.setCommitterName(infoCommit.getAuthor().getName());
                    commitHead.setCommittedAt(infoCommit.getCommitDate());
                    build.setHeadCommit(commitHead);
                } else {
                    AbstractHelper.LOGGER.warn("You reach your rate limit for github, you have to wait "+rateLimit.reset+" to get datas.");
                }

            }

            return build;
        } catch (IOException e) {
            AbstractHelper.LOGGER.warn("Error when getting build id "+id+" : "+e.getMessage());
            return null;
        }
    }
}
