package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.Repository;
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
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.time.format.DateTimeFormatter;

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



    public void readExitFile(String path, String commit, String ghUser, String task, ProjectInspector inspector )  {
        String workspace=System.getProperty("user.dir");
        inspector.getLogger().info( "WORKSPACE "+ System.getProperty("user.dir"));
        Path relativizedPath= Paths.get(workspace).relativize(Paths.get(path));
        FileReader reader = null;
        try {
            reader = new FileReader(relativizedPath.toString());
        } catch (FileNotFoundException e) {
            inspector.getLogger().info("Problem opening the Sorald mine exit file");;
        }
        JSONObject object = null;
        try {
            object = (JSONObject) new JSONParser().parse(reader);
        } catch (IOException | ParseException e) {
            inspector.getLogger().info("Unable to parse the exit File");
        }


        JSONArray minedRules = (JSONArray) object.get("minedRules");
        MongoCollection soboDB =connectToDB(ghUser);
        if (minedRules.isEmpty()){
            noViolationsUpdate(commit, inspector);
        }
        else {

            minedRules.forEach(rul -> parseMinedRules((JSONObject) rul, commit, ghUser, task, soboDB, inspector));
        }
    }

    private boolean blameRepo(ProjectInspector inspector, String path,String commit, String line, String ghUser) {
        Git git = null;
        try {
            git = inspector.openAndGetGitObject();
            org.eclipse.jgit.lib.ObjectId obID= git.getRepository().resolve(commit);
            String newPath=path.replace('\\','/');
            BlameResult blameResult=null;
            try {
                blameResult = git.blame().setFilePath(newPath).setStartCommit(obID).call();
            } catch (GitAPIException e) {
                inspector.getLogger().info("Not able to open GitHub Object");
            }
            if (blameResult != null) {
                if (blameResult.getSourceAuthor(Integer.parseInt(line)-1).getEmailAddress().contains(ghUser)){
                    return true;
                }

            }
        } catch (IOException e) {
            inspector.getLogger().info("Not able to open GitHub Object");
        }
        return false;

    }




    public  void parseMinedRules(JSONObject mRules, String commit, String ghUser, String task, MongoCollection soboDB , ProjectInspector inspector){
        inspector.getLogger().info("PARSED-MINED-RULES");
        JSONArray wLocations= (JSONArray)  mRules.get("warningLocations");
        String ruleKey= (String) mRules.get("ruleKey");
        wLocations.forEach( rul -> parseWarnings( (JSONObject) rul , ruleKey, commit, ghUser,task, soboDB, inspector) );


    }

    public  void parseWarnings(JSONObject warnings, String ruleKey, String commit, String ghUser, String task, MongoCollection soboDB, ProjectInspector inspector){
        String line= warnings.get("startLine").toString();
        String filePath= (String) warnings.get("filePath");


        if (blameRepo(inspector,filePath,commit,line,ghUser)){
            newViolation(soboDB, ghUser,commit,task,filePath,line,ruleKey);
        }



    }

    public MongoCollection connectToDB( String ghUser ){
        String dbUser= System.getenv("dbUser");
        String dbPWD=System.getenv("pwd");
        String IP=System.getenv("IP");
        String BDURI="mongodb://"+dbUser+":"+dbPWD+"@"+IP+":27017";
        MongoConnection mongoConnection = new MongoConnection(BDURI,"sobodb");
        MongoDatabase db =mongoConnection.getMongoDatabase();
        System.out.println("Connecting to "+db.getName() +" from :"+ ghUser+ " repo");
        return db.getCollection(getEnvOrDefault("collection","soboTesting"));}

    public MongoCollection connectToDB( String ghUser, String collectionName ){
        String dbUser= System.getenv("dbUser");
        String dbPWD=System.getenv("pwd");
        String IP=System.getenv("IP");
        String BDURI="mongodb://"+dbUser+":"+dbPWD+"@"+IP+":27017";
        MongoConnection mongoConnection = new MongoConnection(BDURI,"sobodb");
        MongoDatabase db =mongoConnection.getMongoDatabase();
        System.out.println("Connecting to "+collectionName +" from :"+ ghUser+ " repo");
        return db.getCollection(collectionName);}


    /**
     * Method that adds the violation in case it doesn't exist already on the DB
     * @param collection
     * @param ghUser
     * @param commit
     * @param task
     * @param file
     * @param line
     * @param rule
     */
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

    public void noViolationsUpdate(String commit, ProjectInspector inspector){
        StringBuilder issueBody = new StringBuilder();
        GHIssue feedbackIssue = getFeedbackAnalyzerIssue(inspector);
        issueBody.append(":robot: : I couldn't find any violations! :)  \n");
        issueBody.append("## :robot: Excellent work!");
        issueBody.append(" \n"+ "**Working on commit** : "+commit+ " \n");
        try {
            feedbackIssue.setBody(issueBody.toString());
        } catch (IOException e) {
            inspector.getLogger().info("Problem with updating the issue message");
        }
        inspector.getLogger().info("Success!  No violations");

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
        GHIssue feedbackIssue = getFeedbackAnalyzerIssue(inspector);
        if (!mostCommon.hasNext()){

            noViolationsUpdate(commit,inspector);
        }
        else{

        Document comRule= mostCommon.next();
        commonRule= (String) comRule.get("_id");
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
                line = line.replace("{@user}", "@" + user);
                issueBody.append(line).append(System.lineSeparator());
            });

            // Generate table

            issueBody.append("|line | FilePath| \n");
            issueBody.append("|--|--| \n");

            MongoCursor<Document> filePathList = collection.find(filePaths).iterator();
            int numOfFiles = 0;
            while (filePathList.hasNext() && numOfFiles < 10) {
                Document next = filePathList.next();
                String fPath=next.get("filePath").toString().replace("\\", "/");
                String fileClickeable = "["+next.get("filePath")+"](https://gits-15.sys.kth.se/";
                if (fPath.contains("/")){
                    fileClickeable+=inspector.getRepoSlug()+"/tree/"+commit+"/"+fPath+")";
                }else fileClickeable+=inspector.getRepoSlug()+"/blob/"+commit+"/"+fPath+")";
                 issueBody.append("|" + next.get("line") + " | " + fileClickeable + " |" + next.get("rule") +"\n");

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

            issueBody.append(" \n"+ " **Table of Violations found on commit** : "+commit+ " \n");
            DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm, dd MMM uuuu");
            LocalDateTime now = java.time.LocalDateTime.now();
            issueBody.append(" Last update: "+format.format(now.atZone(ZoneId.of("Europe/Stockholm")))+"\n");


            feedbackIssue.setBody(issueBody.toString());
            inspector.getLogger().info("Success!");
            inspector.getLogger().info("Issue succesfully updated on :" + user + "repo");
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

    public void readCommand(ProjectInspector inspector, String user, String task)  {
        GHIssue mainIssue=getCommandIssue(inspector,user);
        if (mainIssue!=null){
        List<GHIssueComment> commentList = getIssueComments(mainIssue);
        if (!commentList.isEmpty()){
            GHIssueComment comment = getLastCommand(commentList);
            String[] command = comment.getBody().split(" ");
            if (command[0] !=null){
                if (command[0].equals(SoboConstants.HELP)) help(inspector, mainIssue);
                else if (command[0].equals(SoboConstants.STOP)) stop(user,task,mainIssue);
                else if (command[0].equals(SoboConstants.GO)) go(user,task, mainIssue);
                else if (command[0].equals(SoboConstants.MORE)) more(inspector, mainIssue , command[1], user , task);
                else if (command[0].equals(SoboConstants.RULE)) warning(inspector, mainIssue , command[1], user , task);
                else {
                    cantReadCommand(mainIssue);
                    inspector.getLogger().info("Can't read command");
                }

        }}}
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
            System.out.println("Can't connect with KTH GH");
        }
        return github;
    }

    /**
     * Method to get the Command Issue created by SOBO in every student repository
     * @param inspector of the instance
     * @return command Issue
     */
    public GHIssue getCommandIssue(ProjectInspector inspector, String user) {
        GitHub github = connectWithGH();

        GHIssue commandIssue = null;
        try {
            GHRepository repo = github.getRepository(inspector.getRepoSlug());
            Iterator<GHIssue> issueIterable = repo.getIssues(GHIssueState.OPEN).iterator();
            for (Iterator<GHIssue> it = issueIterable; it.hasNext(); ) {

                GHIssue issue = it.next();
                String title=issue.getTitle();
                String login=issue.getUser().getLogin();
                if (title.equals(SoboConstants.COMMAND_ISSUE_TITLE) && login.equals(System.getenv("login"))){
                    commandIssue=issue;
                    return commandIssue;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("# 🤖: Hi @"+user+" \n");
            String actualPath= System.getProperty("user.dir");
            String templates= getPath("templates", actualPath);
            Path pathHeader = Paths.get(templates + "/help.md");
            try {
                StringBuilder issueBody = new StringBuilder();
                Files.lines(pathHeader, StandardCharsets.UTF_8).forEach(line -> {
                    // replace all instances of {@user} with the student's name
                    sb.append(line).append(System.lineSeparator());
                });} catch (IOException e) {
                e.printStackTrace();
            }
            sb.append("\n");
            sb.append("🤖: More information on my Github account  [sobo-profile](https://gits-15.sys.kth.se/system-sobo/SOBO-Instructions)) \n");

            sb.append("🤖: Let's start with \\help");

            return repo.createIssue(SoboConstants.COMMAND_ISSUE_TITLE)
                    .body(sb.toString()).create();

        } catch (IOException e) {
            inspector.getLogger().info("Not able to get the repo");
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
                String title=issue.getTitle();
                String login=issue.getUser().getLogin();
                if (title.equals(SoboConstants.FEEDBACK_ISSUE_TITLE) && login.equals(System.getenv("login"))){
                    feedbackIssue=issue;
                    return feedbackIssue;
                }
            }
            return repo.createIssue(SoboConstants.FEEDBACK_ISSUE_TITLE).body("First time generating the automatic message :)").create();

        } catch (IOException e) {
            inspector.getLogger().info("Not able to get the repo");
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
        try {
            GHIssueComment comment = comments.get(lastIndex);
            String login = comment.getUser().getLogin();
            String[] commentBody = comment.getBody().split(" ");
            String command = commentBody[0];
            if (!login.equals("system-sobo") && SoboConstants.COMMANDS.contains(command)){

                return checkComment(login, comment);
            }
        } catch (IOException e) {
            System.out.println("Can´t find comment's User");
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


    private void help(ProjectInspector inspector, GHIssue mainIssue){
        String actualPath= System.getProperty("user.dir");
        String templates= getPath("templates", actualPath);
        Path pathHeader = Paths.get(templates + "/help.md");
        try {
            StringBuilder issueBody = new StringBuilder();
            Files.lines(pathHeader, StandardCharsets.UTF_8).forEach(line -> {
                // replace all instances of {@user} with the student's name
                issueBody.append(line).append(System.lineSeparator());
            });
            mainIssue.comment(issueBody.toString());
        } catch (IOException e) {
            cantReadCommand(mainIssue);
            inspector.getLogger().info("Can't comment issue");
        }

    }
    private void stop(String user, String task, GHIssue issue){
        MongoCollection collection=connectToDB(user, "soboUsersKTH");
        collection.findOneAndUpdate(
                Filters.eq("ghID", user),
                Updates.set(task, false));
        try {
            issue.comment("I will not generate automatic responses for this repo, but remember you can activate me again whenever you want using \\go :) ");
        } catch (IOException e) {
            cantReadCommand(issue);
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
            cantReadCommand(issue);
            System.out.println("Can't send comment message");
        }
    }

    private void cantReadCommand(GHIssue issue){
        try {
            issue.comment("I can't process that command");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void more(ProjectInspector inspector, GHIssue mainIssue, String commit, String user, String task){
        MongoCollection collection = connectToDB(user);
        Bson userRepoFilter = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", user),
                Filters.eq("task", task)
        );
        MongoCursor<Document> violations = collection.find(userRepoFilter).iterator();
        StringBuilder comment= new StringBuilder();

        if(violations.hasNext()){
            comment.append("|line | FilePath| Rule | \n");
            comment.append("|--|--|--| \n");
        while(violations.hasNext()){
            Document next = violations.next();
            String fPath=next.get("filePath").toString().replace("\\", "/");
            String fileClickeable = "["+next.get("filePath")+"](https://gits-15.sys.kth.se/";
            if (fPath.contains("/")){
                fileClickeable+=inspector.getRepoSlug()+"/tree/"+commit+"/"+fPath+")";
            }else fileClickeable+=inspector.getRepoSlug()+"/blob/"+commit+"/"+fPath+")";
            comment.append("|" + next.get("line") + " | " + fileClickeable + " |" + next.get("rule") +"\n");
        }}
        else comment.append("I don't have data about violations on this commit");
        try {
            mainIssue.comment(String.valueOf(comment));
        } catch (IOException e) {
            cantReadCommand(mainIssue);
            inspector.getLogger().info("Error while adding a comment");
        }


    }
    private void warning(ProjectInspector inspector, GHIssue mainIssue,String rule, String user, String task){
        Date date = new Date();
        MongoCollection collection = connectToDB(user);
        Bson userRepoFilter = Filters.and(
                Filters.eq("user", user),
                Filters.eq("task", task),
                Filters.eq("rule", rule)
        );
        MongoCursor<Document> getLastCommit = collection.find(userRepoFilter).sort(Sorts.descending("time")).iterator();
        String commit="";
        if (getLastCommit.hasNext()) {
            Document next = getLastCommit.next();
            commit= next.get("commit").toString();
        }
        Bson ruleCommitFilter = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", user),
                Filters.eq("task", task),
                Filters.eq("rule", rule)
        );
        MongoCursor<Document> violations = collection.find(ruleCommitFilter).iterator();
        StringBuilder comment= new StringBuilder();
        if(violations.hasNext()){
            comment.append("|line | FilePath| Rule | \n");
            comment.append("|--|--|--| \n");
            while(violations.hasNext()){
                Document next = violations.next();
                String fPath=next.get("filePath").toString().replace("\\", "/");
                String fileClickeable = "["+next.get("filePath")+"](https://gits-15.sys.kth.se/";
                if (fPath.contains("/")){
                    fileClickeable+=inspector.getRepoSlug()+"/tree/main/"+fPath+")";
                }else fileClickeable+=inspector.getRepoSlug()+"/blob/main/"+fPath+")";
                comment.append("|" + next.get("line") + " | " + fileClickeable + " |" + next.get("rule") +"\n");

        }}else  comment.append("I don't have data about rule "+rule+" on your last commit");
        try {
            mainIssue.comment(String.valueOf(comment));
        } catch (IOException e) {
            inspector.getLogger().info("Error while adding a comment");
        }


    }

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
