package io.jenkins.plugins.main;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.io.FileOutputStream;
import java.io.IOException;

/* Download file given url and download-to directory */
public class FileDownloader {
	public static void downloadFile(String url,String absoluteFilePath) {
        System.out.println("Downloading from [" + url + "] " + " into " + absoluteFilePath + " .........");
        try {
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(absoluteFilePath);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}