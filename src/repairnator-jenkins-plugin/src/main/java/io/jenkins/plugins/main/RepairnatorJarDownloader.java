package io.jenkins.plugins.main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.File;
import java.io.IOException;

/* Download latest Jar file from snapshot repo */
public class RepairnatorJarDownloader extends FileDownloader{

	public RepairnatorJarDownloader() {}

	public String getLatestJarUrl(String snapshotUrl) throws IOException{
        Document doc = Jsoup.connect(snapshotUrl).get();
        String latestSnapshot = "";
        String latestJar = "";
        for (Element file : doc.select("a[href*=SNAPSHOT]")) {
            latestSnapshot = file.attr("href");
        }

        String jarUrl = snapshotUrl + "/" + latestSnapshot;
        doc = Jsoup.connect(jarUrl).get();
        for (Element file : doc.select("a[href*=jar-with-dependencies]")) {
            latestJar = file.attr("href");
        }

        return jarUrl + "/" +  latestJar;
    }

    public void downloadJar(String snapshotUrl) throws IOException{
        String latestJarUrl = this.getLatestJarUrl(snapshotUrl);
        String absoluteJarFilePath = Config.getInstance().getTempDir().getAbsolutePath() + File.separator + "repairnator.jar";
        this.downloadFile(latestJarUrl,absoluteJarFilePath);
    }
}