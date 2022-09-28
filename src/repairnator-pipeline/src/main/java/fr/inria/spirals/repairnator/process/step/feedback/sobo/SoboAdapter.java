package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import com.mongodb.WriteConcern;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.repository.Repository;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldAdapter;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.jgit.api.Git;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.github.*;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.kohsuke.github.GitHub.connectToEnterprise;
import static org.kohsuke.github.GitHub.connectToEnterpriseWithOAuth;

public class SoboAdapter {

    private static SoboAdapter _instance;
    private String tmpdir;

    public SoboAdapter(String tmpdir){
        this.tmpdir=tmpdir;
    }

    public static SoboAdapter getInstance(String tmpdir) {
        if (_instance == null)
            _instance = new SoboAdapter( tmpdir);
        return _instance;
    }



    public void readExitFile(String path, String commit, String ghUser, String task ) throws IOException, ParseException {
        System.out.println("READ-EXIT-FILE");
        String workspace=System.getProperty("user.dir");
        System.out.println(workspace);
        Path relativizedPath= Paths.get(workspace).relativize(Paths.get(path));
        System.out.println(relativizedPath.toString());
        FileReader reader = new FileReader(relativizedPath.toString());
        JSONObject object =  (JSONObject) new JSONParser().parse(reader);


        JSONArray minedRules = (JSONArray) object.get("minedRules");
        MongoCollection soboDB =connectToDB(commit, ghUser,task);
        minedRules.forEach( rul -> parseMinedRules( (JSONObject) rul , commit,  ghUser,  task , soboDB) );
    }


    public  void parseMinedRules(JSONObject mRules, String commit, String ghUser, String task, MongoCollection soboDB ){
        System.out.println("PARSED-MINED-RULES");
        JSONArray wLocations= (JSONArray)  mRules.get("warningLocations");
        String ruleKey= (String) mRules.get("ruleKey");
        wLocations.forEach( rul -> parseWarnings( (JSONObject) rul , ruleKey, commit, ghUser,task, soboDB) );


    }

    public  void parseWarnings(JSONObject warnings, String ruleKey, String commit, String ghUser, String task, MongoCollection soboDB){
        String line= warnings.get("startLine").toString();
        String filePath= (String) warnings.get("filePath");

        System.out.print("rule " + ruleKey);
        System.out.print(" line " + line);
        System.out.print(" filePath " + filePath);
        System.out.println("task "+task );
        System.out.println("\t");
        addUserViolation(soboDB, ghUser,commit,task,filePath,line,ruleKey);


    }

    public MongoCollection connectToDB(String commit, String ghUser, String task ){
        String dbUser= System.getenv("dbUser");
        String dbPWD=System.getenv("pwd");
        String IP=System.getenv("IP");
        String BDURI="mongodb://"+dbUser+":"+dbPWD+"@"+IP+":27017";
        MongoConnection mongoConnection = new MongoConnection(BDURI,"sobodb");
        System.out.println(mongoConnection.isConnected());
        MongoDatabase db =mongoConnection.getMongoDatabase();
        System.out.println("Inserting commit information for: "+ghUser+" in :"+db.getName());
        MongoCollection collection = db.getCollection("sobodb");
        return collection;}


    public void addUserViolation(MongoCollection collection,String ghUser, String commit, String task, String file, String line, String rule){
        Date date = new Date();

        collection.insertOne(new Document()
                .append("_id", new ObjectId() )
                .append("user", ghUser)
                .append("commit",commit)
                .append("task", task)
                .append("filePath", file)
                .append("line", line)
                .append("rule", rule)
                .append("time", new Timestamp(date.getTime())));

    }

    public void getMostCommonRule(String commit, String user, String task, ProjectInspector inspector) throws IOException {
        MongoCollection collection=connectToDB(commit, user,task);


        Bson userRepoFilter = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", user),
                Filters.eq("task", task)
        );

        //Bson aggregate= (Bson) Arrays.asList(Accumulators.sum("count", 1), userRepoFilter);

        AggregateIterable agregado= collection.aggregate( Arrays.asList(
                Aggregates.match(userRepoFilter),
                Aggregates.group("$rule", Accumulators.sum("count", 1)),
                Aggregates.sort(Sorts.descending("count"))
        ));

        MongoCursor<Document> mostCommon=agregado.iterator();
        String commonRule="";
        if (mostCommon.hasNext()){
            Document next= mostCommon.next();
            commonRule= (String) next.get("_id");
        }


        System.out.println(commonRule);
        Bson filePaths = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", user),
                Filters.eq("rule",commonRule)
        );




        //get the filepath
        StringBuilder sb = new StringBuilder();
        System.out.println(System.getProperty("user.dir"));
        String pathTo109="../../";
        Path pathHeader = Paths.get(pathTo109+ "/" + "HEADER" + ".md");
        Files.lines(pathHeader, StandardCharsets.UTF_8).forEach(line -> {
            // replace all instances of {@user} with the student's name
            line = line.replace("@user", "@"+user);
            sb.append(line).append(System.lineSeparator());
        });

        // table

        sb.append("|line | FilePath| \n");
        sb.append("|--|--| \n");

        MongoCursor<Document> filePathList = collection.find(filePaths).iterator();
        int numOfFiles=0;
        while(filePathList.hasNext() && numOfFiles<10){
            Document next =filePathList.next();
            System.out.println(next.get("filePath")+" - line:"+next.get("line"));
            sb.append("|"+next.get("line")+" |" +next.get("filePath")+"\n");
            numOfFiles++;

        }

        sb.append("\n");

        Path path = Paths.get(pathTo109+ "/" + commonRule + ".md");
        Files.lines(path, StandardCharsets.UTF_8).forEach(line -> {
            // replace all instances of {@user} with the student's name
            line = line.replace("{@user}", "@"+user);
            sb.append(line).append(System.lineSeparator());
        });

        System.out.println(sb.toString());


        GitHub github = connectToEnterpriseWithOAuth("https://gits-15.sys.kth.se/api/v3","sofbob",System.getenv("GOAUTH"));

        GHRepository r = github.getRepository(inspector.getRepoSlug());
        r.createIssue(commonRule+": "+commit.substring(0,4))
                .body(sb.toString()).create();
        System.out.println("Success!");



    }
}
