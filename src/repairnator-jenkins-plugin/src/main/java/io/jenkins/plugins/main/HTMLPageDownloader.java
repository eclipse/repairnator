package io.jenkins.plugins.main;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.StringBuilder;

/* Download HTML Page given url and download-to directory */
public class HTMLPageDownloader implements Downloader{
    private StringBuilder docSb = new StringBuilder();
    private String url;

    public HTMLPageDownloader(String url) {
        this.url = url;
    }

    @Override
	public void download() {
        System.out.println("Downloading from [" + this.url + "]  .........");
        URL realUrl;
        InputStream is = null;
        BufferedReader br;
        String line;
        docSb = new StringBuilder();
        try {
            realUrl = new URL(this.url);
            is = realUrl.openStream(); 
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                docSb.append(line).append("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getHTMLDoc() {
        if (docSb != null ) {
            return docSb.toString();
        }
        return "";
    }


    public String downloadAndGetHTML() {
        this.download();
        return this.getHTMLDoc();
    }
}