package fr.inria.spirals.repairnator;


import fr.inria.spirals.jtravis.entities.Build;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Created by urli on 23/12/2016.
 */
public class Launcher {
    public static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        List<Build> buildList = ProjectScanner.getListOfFailingBuildFromProjects("src/main/resources/project_list.txt");
    }
}
