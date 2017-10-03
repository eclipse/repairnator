import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 05/09/2017.
 */
public class GetTrendingsFromGithub {

    public static void main(String[] args) throws IOException {
        String githubTrendingUrl = "https://github.com/trending/java/?since=weekly";

        Document doc = Jsoup.connect(githubTrendingUrl).get();

        Elements e = doc.getElementsByAttributeValue("class", "explore-content");

        if (e.size() != 1) {
            System.err.println("Error when parsing the page (explore-content)");
            System.exit(-1);
        }

        Elements ol = e.get(0).getElementsByTag("ol");

        if (ol.size() != 1) {
            System.err.println("Error when parsing the page (ol)");
            System.exit(-1);
        }

        Elements h3 = ol.get(0).getElementsByTag("h3");

        List<String> results = new ArrayList<String>();

        System.out.println("Nb trendings: "+h3.size());
        for (Element project : h3) {
            String link = project.getElementsByTag("a").get(0).attr("href");

            String slugName = link.substring(1);

            Repository repo = RepositoryHelper.getRepositoryFromSlug(slugName);
            if (repo != null) {
                Build b = repo.getLastBuild(false);
                if (b != null && b.getBuildTool() == BuildTool.MAVEN) {
                    results.add(slugName);
                }
            }
        }

        System.out.println(StringUtils.join(results,"\n"));
    }
}
