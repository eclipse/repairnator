package io.jenkins.plugins.main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.File;
import java.io.IOException;

/* Download latest Jar file from snapshot repo */
public class RepairnatorJarDownloader implements Downloader{
    private String snapshotUrl;

	public RepairnatorJarDownloader() {}

    public RepairnatorJarDownloader(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }
    /* does not work if maven repo has more than 9 jar uploaded*/
	public String getLatestJarUrl(String snapshotUrl) throws IOException{
        Document doc = Jsoup.connect(snapshotUrl).get();
        String latestSnapshot = "";
        String latestJar = "";
        for (Element file : doc.select("a[href*=SNAPSHOT]")) {
            latestSnapshot = file.attr("href");
        }

        String jarUrl = snapshotUrl + "/" + latestSnapshot;

        HTMLPageDownloader htmlDownloader = new HTMLPageDownloader(jarUrl);
        String docStr = htmlDownloader.downloadAndGetHTML();
        doc = Jsoup.parse(docStr);
        for (Element file : doc.select("a[href*=jar-with-dependencies]")) {
            latestJar = file.attr("href");
        }

        System.out.println("LatestJarUrl: " + jarUrl  + latestJar);
        return jarUrl +  latestJar;
    }

    public void downloadJar(String snapshotUrl) throws IOException{
        String latestJarUrl = this.getLatestJarUrl(snapshotUrl);
        String absoluteJarFilePath = Config.getInstance().getTempDir().getAbsolutePath() + File.separator + "repairnator.jar";

        FileDownloader fileDownloader = new FileDownloader(latestJarUrl,absoluteJarFilePath);
        fileDownloader.download();
    }

    @Override
    public void download() {
        try {
            this.downloadJar(this.snapshotUrl);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}