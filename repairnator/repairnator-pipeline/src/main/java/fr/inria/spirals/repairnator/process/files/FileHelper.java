package fr.inria.spirals.repairnator.process.files;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.step.AbstractStep;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Tellström on 05/07/18.
 */

public class FileHelper {

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FileHelper.class);
    }

    /**
     * Copy the files from a directory into another.
     *
     * @param sourceDir
     *            is the directory containing the files to be copied from.
     * @param targetDir
     *            is the directory where the files from sourceDir are going to be
     *            copied to.
     * @param excludedFileNames
     *            is an optional parameter that may contain a list of file names not
     *            to be included into targetDir.
     * @param isToPerfectlyMatch
     *            is a parameter to be used when excludedFileNames is set with one
     *            or more file names. When isToPerfectlyMatch is set as "true", the
     *            files with the exactly names in excludedFileNames will not be
     *            copied into targetDir. When isToPerfectlyMatch is set as "false",
     *            the file with names containing substring of the names in
     *            excludedFileNames will not be copied into targetDir. For instance,
     *            if excludedFileNames contains the file name ".git", when
     *            isToPerfectlyMatch is true, the file ".gitignore" will be copied
     *            into targetDir, and when isToPerfectlyMatch is false, the file
     *            ".gitignore" will NOT be copied into targetDir.
     * @param step
     *            is the pipeline step from where this method was called (the info
     *            from the step is only used for logging purpose).
     *
     */
    public static void copyDirectory(File sourceDir, File targetDir, String[] excludedFileNames,
            boolean isToPerfectlyMatch, AbstractStep step) {
        getLogger().debug("Copying files...");
        if (sourceDir != null && targetDir != null) {
            getLogger().debug("Source dir: " + sourceDir.getPath());
            getLogger().debug("Target dir: " + targetDir.getPath());

            try {
                FileUtils.copyDirectory(sourceDir, targetDir, new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        for (String excludedFileName : excludedFileNames) {
                            if (isToPerfectlyMatch) {
                                String excludedFilePath = sourceDir.getPath() + "/" + excludedFileName;
                                if (file.getPath().equals(excludedFilePath)) {
                                    getLogger().debug("File not copied: " + file.getPath());
                                    return false;
                                }
                            } else {
                                if (file.getPath().contains(excludedFileName)) {
                                    getLogger().debug("File not copied: " + file.getPath());
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                });
            } catch (IOException e) {
                step.addStepError(
                        "Error while copying files to prepare the git repository folder towards to push data.", e);
            }
        } else {
            step.addStepError(
                    "Error while copying files to prepare the git repository folder towards to push data: the source and/or target folders are null.");
        }
    }

    public static void deleteFile(File file) throws IOException {
        if (file != null) {
            for (File childFile : file.listFiles()) {
                if (childFile.isDirectory()) {
                    deleteFile(childFile);
                } else {
                    if (!childFile.delete()) {
                        throw new IOException();
                    }
                }
            }
            if (!file.delete()) {
                throw new IOException();
            }
        }
    }

    public static void removeNotificationFromTravisYML(File directory, AbstractStep step) {
        File travisFile = new File(directory, Utils.TRAVIS_FILE);

        if (!travisFile.exists()) {
            getLogger().warn("Travis file has not been detected. It should however exists.");
        } else {
            try {
                List<String> lines = Files.readAllLines(travisFile.toPath());
                List<String> newLines = new ArrayList<>();
                boolean changed = false;
                boolean inNotifBlock = false;

                for (String line : lines) {
                    if (line.trim().equals("notifications:")) {
                        changed = true;
                        inNotifBlock = true;
                    }
                    if (inNotifBlock) {
                        if (line.trim().isEmpty()) {
                            inNotifBlock = false;
                            newLines.add(line);
                        } else {
                            newLines.add("#" + line);
                        }
                    } else {
                        newLines.add(line);
                    }
                }

                if (changed) {
                    getLogger().info("Notification block detected. The travis file will be changed.");
                    File bakTravis = new File(directory, "bak" + Utils.TRAVIS_FILE);
                    Files.deleteIfExists(bakTravis.toPath());
                    Files.move(travisFile.toPath(), bakTravis.toPath());
                    FileWriter fw = new FileWriter(travisFile);
                    for (String line : newLines) {
                        fw.append(line);
                        fw.append("\n");
                        fw.flush();
                    }
                    fw.close();

                    step.getInspector().getJobStatus().addFileToPush(Utils.TRAVIS_FILE);
                    step.getInspector().getJobStatus().addFileToPush("bak" + Utils.TRAVIS_FILE);
                }
            } catch (IOException e) {
                getLogger().warn("Error while changing travis file", e);
            }
        }
    }

    public static void removeGhOauthFromCreatedFilesToPush(File directory, List<String> fileNames) {
        String ghOauthPattern = "--ghOauth\\s+[\\w]+";
        for (String fileName : fileNames) {
            File file = new File(directory, fileName);

            if (!file.exists()) {
                getLogger().warn("The file " + file.toPath() + " does not exist.");
            } else {
                Charset charset = StandardCharsets.UTF_8;
                try {
                    String content = new String(Files.readAllBytes(file.toPath()), charset);
                    String updatedContent = content.replaceAll(ghOauthPattern, "[REMOVED]");
                    if (!content.equals(updatedContent)) {
                        getLogger().info(
                                "ghOauth info detected in file " + file.toPath() + ". Such file will be changed.");
                        Files.write(file.toPath(), updatedContent.getBytes(charset));
                    }
                } catch (IOException e) {
                    getLogger().warn("Error while checking if file " + file.toPath() + " contains ghOauth info.", e);
                }
            }
        }
    }
}
