package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.jtravis.entities.Repository;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Created by urli on 04/01/2017.
 */
public class PRInformationHelper extends AbstractHelper {

    private static PRInformationHelper instance;

    private PRInformationHelper() {
        super();
    }

    private static PRInformationHelper getInstance() {
        if (instance == null) {
            instance = new PRInformationHelper();
        }
        return instance;
    }

    public static PRInformation getPRInformationFromBuild(Build build) {
        try {
            if (build.isPullRequest()) {
                GitHub github = getInstance().getGithub();
                GHRateLimit rateLimit = github.getRateLimit();
                SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
                getInstance().getLogger().debug("GitHub ratelimit: Limit: " + rateLimit.limit + " Remaining: " + rateLimit.remaining + " Reset hour: " + dateFormat.format(rateLimit.reset));

                if (rateLimit.remaining > 2) {
                    GHRepository ghRepo = github.getRepository(build.getRepository().getSlug());
                    GHPullRequest pullRequest = ghRepo.getPullRequest(build.getPullRequestNumber());

                    PRInformation prInformation = new PRInformation();

                    GHCommitPointer base = pullRequest.getBase();
                    GHCommitPointer head = pullRequest.getHead();

                    GHRepository headRepo = head.getRepository();

                    Repository repoPR = new Repository();
                    repoPR.setId(headRepo.getId());
                    repoPR.setDescription(headRepo.getDescription());
                    repoPR.setActive(true);
                    repoPR.setSlug(headRepo.getFullName());

                    prInformation.setOtherRepo(repoPR);

                    Commit commitHead = new Commit();
                    commitHead.setSha(head.getSha());
                    commitHead.setBranch(head.getRef());
                    commitHead.setCompareUrl(head.getCommit().getHtmlUrl().toString());

                    GHCommit.ShortInfo infoCommit = head.getCommit().getCommitShortInfo();

                    commitHead.setMessage(infoCommit.getMessage());
                    commitHead.setCommitterEmail(infoCommit.getAuthor().getEmail());
                    commitHead.setCommitterName(infoCommit.getAuthor().getName());
                    commitHead.setCommittedAt(infoCommit.getCommitDate());
                    prInformation.setHead(commitHead);

                    Commit commitBase = new Commit();
                    commitBase.setSha(base.getSha());
                    commitBase.setBranch(base.getRef());
                    commitBase.setCompareUrl(base.getCommit().getHtmlUrl().toString());

                    infoCommit = base.getCommit().getCommitShortInfo();

                    commitBase.setMessage(infoCommit.getMessage());
                    commitBase.setCommitterEmail(infoCommit.getAuthor().getEmail());
                    commitBase.setCommitterName(infoCommit.getAuthor().getName());
                    commitBase.setCommittedAt(infoCommit.getCommitDate());
                    prInformation.setBase(commitBase);

                    return prInformation;
                } else {
                    getInstance().getLogger().warn("You reach your rate limit for github, you have to wait " + rateLimit.reset + " to get datas. PRInformation will be null for build "+build.getId());
                }
            } else {
                getInstance().getLogger().info("Getting PRInformation return null for build id "+build.getId()+" as it does not come from a PR.");
            }
        } catch (IOException e) {
            getInstance().getLogger().warn("Error when getting PRInformation for build id "+build.getId()+" : "+e.getMessage());
        }
        return null;
    }
}
