import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

/**
 * Created by urli on 05/09/2017.
 */
public class CleanProjectList {

    public static void main(String[] args) throws IOException {
        String projectPath = args[0];
        String dbUrl = args[1];
        String dbName = args[2];
        String collectionName = args[3];
        String destList = args[4];

        List<String> allProjects = Files.readAllLines(new File(projectPath).toPath());
        MongoConnection mongoConnection = new MongoConnection(dbUrl, dbName);
        MongoDatabase database = mongoConnection.getMongoDatabase();
        MongoCollection collection = database.getCollection(collectionName);

        List<String> selectedProjects = new ArrayList<>();
        for (String project : allProjects) {
            Repository repo = RepositoryHelper.getRepositoryFromSlug(project);
            if (repo != null) {
                Build b = repo.getLastBuild(false);
                if (b != null) {
                    if (b.getBuildTool() == BuildTool.MAVEN) {
                        long results = collection.count(and(
                                eq("repositoryName", project),
                                ne("typeOfFailures", null)
                        ));

                        if (results > 0) {
                            selectedProjects.add(project);
                        }
                    }
                }
            }
        }

        File outputFile = new File(destList);
        BufferedWriter buffer = new BufferedWriter(new FileWriter(outputFile));
        buffer.write(StringUtils.join(selectedProjects,"\n"));
        buffer.close();

        System.out.println("Read projects: "+allProjects.size()+" | Selected projects : "+selectedProjects.size());
        System.out.println(StringUtils.join(selectedProjects, "\n"));
    }
}
