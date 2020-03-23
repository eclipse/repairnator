package io.jenkins.plugins.main;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.io.FileOutputStream;
import java.io.IOException;

/* Download file given url and download-to directory */
public class FileDownloader implements Downloader{
    private String url;
    private String absoluteFilePath;

    public FileDownloader(String url,String absoluteFilePath) {
        this.url = url;
        this.absoluteFilePath = absoluteFilePath;
    }

    @Override
	public void download() {
        System.out.println("Downloading from [" + this.url + "] " + " into " + this.absoluteFilePath + " .........");
        try {
            URL website = new URL(this.url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(this.absoluteFilePath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}