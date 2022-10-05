package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.github.*;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;

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



    public void readExitFile(String path, String commit, String ghUser, String task )  {
        System.out.println("READ-EXIT-FILE");
        String workspace=System.getProperty("user.dir");
        System.out.println(workspace);
        Path relativizedPath= Paths.get(workspace).relativize(Paths.get(path));
        System.out.println(relativizedPath.toString());
        FileReader reader = null;
        try {
            reader = new FileReader(relativizedPath.toString());
        } catch (FileNotFoundException e) {
            System.out.println("Problem opening the Sorald mine exit file");;
        }
        JSONObject object = null;
        try {
            object = (JSONObject) new JSONParser().parse(reader);
        } catch (IOException | ParseException e) {
            System.out.println("Unable to parse the exit File");
        }


        JSONArray minedRules = (JSONArray) object.get("minedRules");
        MongoCollection soboDB =connectToDB(ghUser);
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
        newViolation(soboDB, ghUser,commit,task,filePath,line,ruleKey);


    }

    public MongoCollection connectToDB( String ghUser ){
        String dbUser= System.getenv("dbUser");
        String dbPWD=System.getenv("pwd");
        String IP=System.getenv("IP");
        String BDURI="mongodb://"+dbUser+":"+dbPWD+"@"+IP+":27017";
        MongoConnection mongoConnection = new MongoConnection(BDURI,"sobodb");
        System.out.println(mongoConnection.isConnected());
        MongoDatabase db =mongoConnection.getMongoDatabase();
        System.out.println("Inserting commit information for: "+ghUser+" in :"+db.getName());
        return db.getCollection(getEnvOrDefault("collection","soboTesting"));}

    public MongoCollection connectToDB( String ghUser, String collectionName ){
        String dbUser= System.getenv("dbUser");
        String dbPWD=System.getenv("pwd");
        String IP=System.getenv("IP");
        String BDURI="mongodb://"+dbUser+":"+dbPWD+"@"+IP+":27017";
        MongoConnection mongoConnection = new MongoConnection(BDURI,"sobodb");
        System.out.println(mongoConnection.isConnected());
        MongoDatabase db =mongoConnection.getMongoDatabase();
        System.out.println("Inserting commit information for: "+ghUser+" in :"+db.getName());
        return db.getCollection(collectionName);}



    public void newViolation(MongoCollection collection,String ghUser, String commit, String task, String file, String line, String rule){
        Bson userRepoFilter = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", ghUser),
                Filters.eq("task", task),
                Filters.eq("filePath", file),
                Filters.eq("line", line),
                Filters.eq("rule", rule)
        );

        MongoIterable duplicate = collection.find(userRepoFilter);
        if(!duplicate.iterator().hasNext()){
            addUserViolation(collection, ghUser,commit,task,file,line,rule);}
        else
            System.out.println("Violation already in the DB");
    }

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
        MongoCollection collection=connectToDB(user);


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
        StringBuilder issueBody = new StringBuilder();
        GitHub github = connectWithGH();
        GHIssue feedbackIssue = getFeedbackAnalyzerIssue(inspector);
        GHRepository r = github.getRepository(inspector.getRepoSlug());
        if (!mostCommon.hasNext()){


            issueBody.append("No violations :) "+"-- On commit : "+commit.substring(0,6)+ "\n");
            issueBody.append("Excelent work!");
            feedbackIssue.setBody(issueBody.toString());
            System.out.println("Success!  No violations");
        }
        else{

        Document comRule= mostCommon.next();
        commonRule= (String) comRule.get("_id");
        System.out.println(commonRule);
        Bson filePaths = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", user),
                Filters.eq("rule",commonRule)
        );




            //crate the String Builder
            String actualPath= System.getProperty("user.dir");
            String pathToHeader= getPath("header", actualPath);
            Path pathHeader = Paths.get(pathToHeader + "/" + commonRule +".md");
            Files.lines(pathHeader, StandardCharsets.UTF_8).forEach(line -> {
                // replace all instances of {@user} with the student's name
                line = line.replace("@user", "@" + user);
                issueBody.append(line).append(System.lineSeparator());
            });

            // Generate table

            issueBody.append("|line | FilePath| \n");
            issueBody.append("|--|--| \n");

            MongoCursor<Document> filePathList = collection.find(filePaths).iterator();
            int numOfFiles = 0;
            while (filePathList.hasNext() && numOfFiles < 10) {
                Document next = filePathList.next();
                System.out.println(next.get("filePath") + " - line:" + next.get("line"));
                issueBody.append("|" + next.get("line") + " |" + next.get("filePath") + "\n");
                numOfFiles++;

            }

            issueBody.append("\n");

            String pathToBody= getPath("body", actualPath);
            Path pathBody = Paths.get(pathToBody + "/" + commonRule +".md");
            Files.lines(pathBody, StandardCharsets.UTF_8).forEach(line -> {
                // replace all instances of {@user} with the student's name
                line = line.replace("{@user}", "@" + user);
                issueBody.append(line).append(System.lineSeparator());
            });

            feedbackIssue.setBody(issueBody.toString());
            System.out.println("Success!");
            System.out.println("Issue succesfully updated on :" + user + "repo");
        }
    }






    private static String getPath(String drl, String whereIAm) {
        File dir = new File(whereIAm); //StaticMethods.currentPath() + "\\src\\main\\resources\\" +
        for(File e : dir.listFiles()) {
            if(e.getName().equals(drl)) {return e.getPath();}
            if(e.isDirectory()) {
                String idiot = getPath(drl, e.getPath());
                if(idiot != null) {return idiot;}
            }
        }
        return null;
    }

    public void readCommand(ProjectInspector inspector, String user, String task) throws Exception {
        GHIssue mainIssue=getCommandIssue(inspector);
        if (mainIssue!=null){
        List<GHIssueComment> commentList = getIssueComments(mainIssue);
        GHIssueComment comment = getLastCommand(commentList);
        String[] command = comment.getBody().split(" ");
        if (command[0] !=null){
            if (command[0].equals(SoboConstants.HELP)) help();
            else if (command[0].equals(SoboConstants.STOP)) stop(user,task,mainIssue);
            else if (command[0].equals(SoboConstants.GO)) go(user,task, mainIssue);
            else if (command[0].equals(SoboConstants.MORE)) more();
            else if (command[0].equals(SoboConstants.RULE)) warning();
            else {
                throw new Exception("Not a valid Command");
            }

        }}
    }



    /**
     * Method to connect with Github, specially if is GitHub Enterprise
     * TODO: add the not Enterprise method
     * @return Github connection
     */
    public GitHub connectWithGH(){
        GitHub github = null;
        try {
           github = connectToEnterpriseWithOAuth("https://gits-15.sys.kth.se/api/v3", System.getenv("login"), System.getenv("GOAUTH"));

        } catch (IOException e) {
            System.out.println();
        }
        return github;
    }

    /**
     * Method to get the Command Issue created by SOBO in every student repository
     * @param inspector of the instance
     * @return command Issue
     */
    public GHIssue getCommandIssue(ProjectInspector inspector) {
        GitHub github = connectWithGH();

        GHIssue commandIssue = null;
        try {
            GHRepository repo = github.getRepository(inspector.getRepoSlug());
            Iterator<GHIssue> issueIterable = repo.getIssues(GHIssueState.OPEN).iterator();
            for (Iterator<GHIssue> it = issueIterable; it.hasNext(); ) {

                GHIssue issue = it.next();
                System.out.println(issue.getTitle() + " made by : "+ issue.getUser().getName());
                String title=issue.getTitle();
                String login=issue.getUser().getLogin();
                if (title.equals(SoboConstants.COMMAND_ISSUE_TITLE) && login.equals("system-sobo")){
                    commandIssue=issue;
                    return commandIssue;
                }
            }
            repo.createIssue(SoboConstants.COMMAND_ISSUE_TITLE)
                    .body("I will listen your commands on this issue").create();

        } catch (IOException e) {
            System.out.println("Not able to get the repo");
        }

        return commandIssue;
    }

    public GHIssue getFeedbackAnalyzerIssue(ProjectInspector inspector) {
        GitHub github = connectWithGH();

        GHIssue feedbackIssue = null;
        try {
            GHRepository repo = github.getRepository(inspector.getRepoSlug());
            Iterator<GHIssue> issueIterable = repo.getIssues(GHIssueState.OPEN).iterator();
            for (Iterator<GHIssue> it = issueIterable; it.hasNext(); ) {

                GHIssue issue = it.next();
                System.out.println(issue.getTitle() + " made by : "+ issue.getUser().getName());
                String title=issue.getTitle();
                String login=issue.getUser().getLogin();
                if (title.equals(SoboConstants.FEEDBACK_ISSUE_TITLE) && login.equals("system-sobo")){
                    feedbackIssue=issue;
                    return feedbackIssue;
                }
            }
            repo.createIssue(SoboConstants.FEEDBACK_ISSUE_TITLE).create();

        } catch (IOException e) {
            System.out.println("Not able to get the repo");
        }

        return feedbackIssue;
    }

    /**
     * Method to get the comments in a particular Issue
     * @param issue working on
     * @return List of comments
     */
    public List<GHIssueComment> getIssueComments(GHIssue issue){
        try {

            return issue.getComments();
        } catch (IOException e) {
            System.out.println("Can't get issue's comments");
        }
        return null;
    }

    /**
     * method to get the last command on an Issue
     * @param comments List of comments
     * @return the laast comment
     * @throws IOException if can't get the user
     */
    public GHIssueComment getLastCommand(List<GHIssueComment> comments) {
        int lastIndex = comments.size()-1;
        for(int i=lastIndex; i>-1; i--) {
            try {
                GHIssueComment comment = comments.get(i);
                String login = comment.getUser().getLogin();
                String[] commentBody = comment.getBody().split(" ");
                String command = commentBody[0];
                if (!login.equals("system-sobo") && SoboConstants.COMMANDS.contains(command)){

                    return checkComment(login, comment);
                }
            } catch (IOException e) {
                System.out.println("Can´t find comment's User");;
            }

        }
        return null;
    }

    public GHIssueComment checkComment(String user, GHIssueComment comment) throws IOException {
        MongoCollection collection= connectToDB(user,"sobodbCommand");

        long time = comment.getCreatedAt().getTime();
        String repoName = comment.getParent().getRepository().getName();
        long issueID = comment.getParent().getId();
        String command=comment.getBody().split(" ")[0];
        String body=comment.getBody();

        Bson commandFilter = Filters.and(
                Filters.eq("command", command),
                Filters.eq("body", body),
                Filters.eq("timestamp", time),
                Filters.eq("user", user),
                Filters.eq("repo", repoName),
                Filters.eq("issue", issueID));
        MongoIterable duplicate = collection.find(commandFilter);
        if(!duplicate.iterator().hasNext()){
            addUserCommand(collection, command, body,time,user,repoName,issueID);
            return comment;}
        else
            System.out.println("No new commands");
            return null;


    }

    public void addUserCommand(MongoCollection collection, String command, String body, long time,String user,String repoName,long issueID){

        collection.insertOne(new Document()
                .append("command", command)
                .append("body", body)
                .append("timestamp", time)
                .append("user", user)
                .append("repo", repoName)
                .append("issue", issueID));

    }

    private static String getEnvOrDefault(String name, String dfault) {
        String env = System.getenv(name);
        if (env == null || env.equals(""))
            return dfault;

        return env;
    }


    private void help(){

    }
    private void stop(String user, String task, GHIssue issue){
        MongoCollection collection=connectToDB(user, "soboUsersKTH");
        collection.findOneAndUpdate(
                Filters.eq("ghID", user),
                Updates.set(task, false));
        try {
            issue.comment("I will not generate automatic responses for this repo, but remember you can activate me again whenever you want :) ");
        } catch (IOException e) {
            System.out.println("Can't send comment message");
        }

    }
    private void go(String user, String task, GHIssue issue){
        MongoCollection collection=connectToDB(user, "soboUsersKTH");
        collection.findOneAndUpdate(
                Filters.eq("ghID", user),
                Updates.set(task, true));
        try {
            issue.comment("Hello again! ");
        } catch (IOException e) {
            System.out.println("Can't send comment message");
        }
    }

    private void more(){

    }
    private void warning(){}

    public boolean checkUser(String userName) {
        MongoCollection collection=connectToDB(userName, "soboUsersKTH");
        Bson userFilter = Filters.and(
                Filters.eq("ghID", userName)
        );
        MongoIterable duplicate = collection.find(userFilter);
        return duplicate.iterator().hasNext();
    }

    public boolean checkUserRepo(String userName, String task) {
        MongoCollection collection=connectToDB(userName, "soboUsersKTH");


        Bson userTaskFilter = Filters.and(
                Filters.eq("ghID", userName),
                Filters.eq(task, true)
        );
        MongoIterable duplicate = collection.find(userTaskFilter);
        return duplicate.iterator().hasNext();
    }
}
