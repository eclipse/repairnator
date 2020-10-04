package io.jenkins.plugins.main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.File;
import java.io.IOException;

/* Download latest Jar file from snapshot repo */
public class RepairnatorJarDownloader implements Downloader{
    private String snapshotUrl;
    private String absoluteJarFilePath;

	public RepairnatorJarDownloader() {}

    public RepairnatorJarDownloader(String snapshotUrl,String absoluteJarFilePath) {
        this.snapshotUrl = snapshotUrl;
        this.absoluteJarFilePath = absoluteJarFilePath;
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
            if (file.attr("href").contains("SNAPSHOT")) {
                latestSnapshot = file.attr("href");
            }
        }

        System.out.println(latestSnapshot);

        HTMLPageDownloader htmlDownloader = new HTMLPageDownloader(latestSnapshot);
        String docStr = htmlDownloader.downloadAndGetHTML();
        doc = Jsoup.parse(docStr);
        for (Element file : doc.select("a[href*=jar-with-dependencies]")) {
            if (file.attr("href").endsWith("jar-with-dependencies.jar")) {
                latestJar = file.attr("href");
            }
        }
        return latestJar;
    }

    public void downloadJar(String snapshotUrl_in,String absoluteJarFilePath_in) throws IOException{
        String latestJarUrl = this.getLatestJarUrl(snapshotUrl_in);
        System.out.println(absoluteJarFilePath_in);
        FileDownloader fileDownloader = new FileDownloader(latestJarUrl,absoluteJarFilePath_in);
        fileDownloader.download();
    }

    public void downloadJarHardCoded(String latestJarUrl) throws IOException { 
        FileDownloader fileDownloader = new FileDownloader(latestJarUrl,this.absoluteJarFilePath);
        fileDownloader.download();
    }

    @Override
    public void download() {
        try {
            this.downloadJar(this.snapshotUrl,this.absoluteJarFilePath);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}