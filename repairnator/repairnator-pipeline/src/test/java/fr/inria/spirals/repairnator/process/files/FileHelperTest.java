package fr.inria.spirals.repairnator.process.files;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;

public class FileHelperTest {

    private File tmpDir;

    @After
    public void tearDown() throws IOException {
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    public void testRemoveNotificationFromTravisYML() throws IOException {
        File resourceFile = new File("./src/test/resources/travis-file/.travis.yml");
        tmpDir = Files.createTempDirectory("test_removeNotificationFromTravisYML").toFile();
        FileUtils.copyFileToDirectory(resourceFile, tmpDir);

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath());
        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir.getAbsolutePath());
        CloneRepository cloneStep = new CloneRepository(inspector);

        FileHelper.removeNotificationFromTravisYML(tmpDir, cloneStep);

        File bak = new File(tmpDir.getAbsolutePath() + "/bak.travis.yml");
        File travis = new File(tmpDir.getAbsolutePath() + "/.travis.yml");

        assertTrue(bak.exists());
        assertTrue(travis.exists());

        List<String> lines = Files.readAllLines(travis.toPath());
        int i;
        for (i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("notification"))
                break;
        }
        assertTrue(nextLevelIsLowestLevel(lines, getIndexOfFirstLineAfterConsecutiveHashes(lines, i)));
    }

    public static boolean lineBeginsWithHash(List<String> lines, int index) {
        String line = lines.get(index);
        if (line.trim().startsWith("#")) {
            return true;
        } else {
            return false;
        }
    }

    public static int getIndexOfFirstLineAfterConsecutiveHashes(List<String> lines, int index) {
        int returnIndex = index;
        while (lineBeginsWithHash(lines, returnIndex)) {
            returnIndex++;
        }
        return returnIndex;
    }

    public static boolean nextLevelIsLowestLevel(List<String> lines, int index) {
        String line = lines.get(index);
        while ((line.length() == 0) && (index < lines.size())) {
            index++;
            line = lines.get(index);
        }
        if (index < lines.size()) {
            if (line.charAt(0) == ' ') {
                return false;
            } else {
                return true;
            }
        }
        else{
            return true;
        }
    }

}
