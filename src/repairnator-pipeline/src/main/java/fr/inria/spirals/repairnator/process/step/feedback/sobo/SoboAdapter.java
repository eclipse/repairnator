package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.github.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.kohsuke.github.GitHub;
import org.slf4j.Logger;

import static org.kohsuke.github.GitHub.connectToEnterpriseWithOAuth;

public class SoboAdapter {
    private MongoCollection<Document> minedViolations;
    private MongoCollection<Document> soboAM;
    private  MongoCollection soboCL;
    private  MongoCollection<Document> commands;
    private MongoCollection users;

    private static SoboAdapter _instance;
    private String tmpdir;
    private Date commitDate;
    private String task;

    private String commitSHA;
    private ProjectInspector inspector;
    private Logger logger;

    public DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm, dd MMM uuuu");
    public LocalDateTime now = java.time.LocalDateTime.now();

    private String commitAuthor;
    private String mineViolationColl ="minedViolations";
    private String commandColl="commands";
    private String AMColl="soboAM";
    private String CLColl="soboCL";
    private String UsersColl="KTHUsers";
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

    public SoboAdapter(String tmpdir){
        this.tmpdir=tmpdir;
    }

    public static SoboAdapter getInstance(String tmpdir) {
        if (_instance == null)
            _instance = new SoboAdapter( tmpdir);
        return _instance;
    }



    public void readExitFile(String path, String commit, String ghUserCommitAuthor, String task, ProjectInspector inspector, Date commitDate )  {
        String workspace=System.getProperty("user.dir");
        this.inspector=inspector;
        this.logger=inspector.getLogger();
        this.inspector.getLogger().info( "WORKSPACE "+ System.getProperty("user.dir"));
        Path relativizedPath= Paths.get(workspace).relativize(Paths.get(path));
        FileReader reader = null;
        this.commitDate=commitDate;
        this.task=task;
        this.commitSHA=commit;
        this.inspector=inspector;
        this.commitAuthor=ghUserCommitAuthor;
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
        this.minedViolations =connectToDB(ghUserCommitAuthor, mineViolationColl);
        this.soboAM= connectToDB(ghUserCommitAuthor, AMColl);

        if (minedRules.isEmpty()){
            newClearCommit(ghUserCommitAuthor,commit,task);
            noViolationsAM(commit, inspector);
        }
        else {

            minedRules.forEach(rul -> parseMinedRules((JSONObject) rul, commit, ghUserCommitAuthor, task, inspector));
        }
    }

    public MongoCollection<Document> connectToDB(String ghUser, String collectionName ){
        String dbUser= System.getenv("dbUser");
        String dbPWD=System.getenv("pwd");
        String IP=System.getenv("IP");
        String BDURI="mongodb://"+dbUser+":"+dbPWD+"@"+IP+":27017";         //database
        MongoConnection mongoConnection = new MongoConnection(BDURI,"sobodb");
        MongoDatabase db =mongoConnection.getMongoDatabase();
        System.out.println("Connecting to "+collectionName +" from :"+ ghUser+ " repo");
        return db.getCollection(collectionName);}

    /**
     * Method to add clear Commit on the soboClearCommits Database
     * @param ghUser
     * @param commit
     * @param task
     */
    public void newClearCommit(String ghUser, String commit, String task){
        String rule_list= System.getenv("SONAR_RULES")!=null? System.getenv("SONAR_RULES"): SoboConstants.RULES_SLOT_1 ;
        Bson userRepoFilter = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", ghUser),
                Filters.eq("task", task),
                Filters.eq("rule_list", rule_list),
                Filters.eq("clear", true)
        );

        MongoIterable<Document> duplicate = minedViolations.find(userRepoFilter);
        if(!duplicate.iterator().hasNext()){
            addClearCommit( ghUser,commit,task, rule_list);}
        else System.out.println("DATA already in the DB");
    }


    public void addClearCommit(String ghUser, String commit, String task, String rule_list){

        minedViolations.insertOne(new Document()
                .append("_id", new ObjectId() )
                .append("user", ghUser)
                .append("commit",commit)
                .append("task", task)
                .append("rule_list", rule_list)
                .append("clear", true)
                .append("time", commitDate.getTime()));

    }


    /**
     * Method that updates the AM Issue on students repository
     * @param commit to specify where the information was found
     * @param inspector to get the issue
     */
    public void noViolationsAM(String commit, ProjectInspector inspector){
        StringBuilder issueBody = new StringBuilder();
        GHIssue feedbackIssue = getFeedbackAnalyzerIssue(inspector);
        issueBody.append(":robot: : I couldn't find any violations! :)  \n");
        issueBody.append("## :robot: Excellent work!");
        issueBody.append(" \n"+ "**Working on commit** : "+commit+ " \n");
        issueBody.append(" Last update: "+format.format(now.atZone(ZoneId.of("Europe/Stockholm")))+"\n");
        try {
            feedbackIssue.setBody(issueBody.toString());
            newAMClearCommit();

        } catch (IOException e) {
            inspector.getLogger().info("Problem with updating the issue message");
        }
        inspector.getLogger().info("Success!  No violations");


    }

    private void newAMClearCommit() {
        String rule_list= System.getenv("SONAR_RULES")!=null? System.getenv("SONAR_RULES"): SoboConstants.RULES_SLOT_1 ;
        Bson userRepoFilter = Filters.and(
                Filters.eq("commit", commitSHA),
                Filters.eq("user", commitAuthor),
                Filters.eq("task", task),
                Filters.eq("rule_list", rule_list),
                Filters.eq("clear", true)
        );

        MongoIterable<Document> duplicate = soboAM.find(userRepoFilter);
        if(!duplicate.iterator().hasNext()){
            addAMClearCommit( rule_list);}
        else System.out.println("DATA already in the DB");

    }

    private void addAMClearCommit(String rule_list) {
        soboAM.insertOne(new Document()
                .append("_id", new ObjectId() )
                .append("user", commitAuthor)
                .append("commit",commitSHA)
                .append("task", task)
                .append("rule_list", rule_list)
                .append("clear", true)
                .append("time", timestamp.getTime()));
    }


    public  void parseMinedRules(JSONObject mRules, String commit, String ghUser, String task, ProjectInspector inspector){
        inspector.getLogger().info("PARSED-MINED-RULES");
        JSONArray wLocations= (JSONArray)  mRules.get("warningLocations");
        String ruleKey= (String) mRules.get("ruleKey");
        wLocations.forEach( rul -> parseViolations( (JSONObject) rul , ruleKey, commit, ghUser,task, inspector) );


    }

    public  void parseViolations(JSONObject warnings, String ruleKey, String commit, String ghUser, String task, ProjectInspector inspector){
        String line= warnings.get("startLine").toString();
        String filePath= (String) warnings.get("filePath");

        if (blameRepo(inspector,filePath,commit,line,ghUser)){
            newViolation( ghUser,commit,task,filePath,line,ruleKey);
        }
    }

    private boolean blameRepo(ProjectInspector inspector, String path,String commit, String line, String ghUser) {
        Git git ;
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
        } catch (Exception e) {
            inspector.getLogger().info("Not able to open GitHub Object");
        }
        return false;

    }

    /**
     * Method that adds the violation in case it doesn't exist already on the DB
     * @param ghUser
     * @param commit
     * @param task
     * @param file
     * @param line
     * @param rule
     */
    public void newViolation(String ghUser, String commit, String task, String file, String line, String rule){
        Bson userRepoFilter = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", ghUser),
                Filters.eq("task", task),
                Filters.eq("filePath", file),
                Filters.eq("line", line),
                Filters.eq("rule", rule)
        );

        MongoIterable<Document> duplicate = minedViolations.find(userRepoFilter);
        if(!duplicate.iterator().hasNext()){
            addUserViolation(minedViolations, ghUser,commit,task,file,line,rule);}
        else
            inspector.getLogger().info("Violation already in the DB");
    }


    public void addUserViolation(MongoCollection<Document> collection, String ghUser, String commit, String task, String file, String line, String rule){
        collection.insertOne(new Document()
            .append("_id", new ObjectId() )
            .append("user", ghUser)
            .append("commit",commit)
            .append("task", task)
            .append("filePath", file)
            .append("line", line)
            .append("rule", rule)
            .append("time", commitDate.getTime()));

    }






    public void getMostCommonRule(String commit, String user, String task, ProjectInspector inspector) throws IOException {


        Bson userRepoFilter = Filters.and(
                Filters.eq("commit", commit),
                Filters.eq("user", user),
                Filters.eq("task", task)
        );

        //Bson aggregate= (Bson) Arrays.asList(Accumulators.sum("count", 1), userRepoFilter);

        AggregateIterable<Document> agregado= minedViolations.aggregate( Arrays.asList(
                Aggregates.match(userRepoFilter),
                Aggregates.group("$rule", Accumulators.sum("count", 1)),
                Aggregates.sort(Sorts.descending("count"))
        ));

        MongoCursor<Document> mostCommon=agregado.iterator();
        String commonRule="";
        StringBuilder issueBody = new StringBuilder();
        GHIssue feedbackIssue = getFeedbackAnalyzerIssue(inspector);
        if (!mostCommon.hasNext() ){

            newClearCommit(user,commit,task);
            noViolationsAM(commit,inspector);

        }
        //remove if not testing 2!=0 &&
            Document comRule= mostCommon.next();
            commonRule= (String) comRule.get("_id");
        if (commonRule ==null ){
            newClearCommit(user,commit,task);
            noViolationsAM(commit,inspector);
        }
            Bson filePaths = Filters.and(
                    Filters.eq("commit", commit),
                    Filters.eq("user", user),
                    Filters.eq("rule",commonRule)
            );
            

            //create the String Builder
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

            MongoCursor<Document> filePathList = minedViolations.find(filePaths).iterator();
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
            issueBody.append(" Last update: "+format.format(now.atZone(ZoneId.of("Europe/Stockholm")))+"\n");


            feedbackIssue.setBody(issueBody.toString());
            newAMMinedCommit(commonRule);
            inspector.getLogger().info("Success!");
            inspector.getLogger().info("Issue succesfully updated on :" + user + "repo");
        }


    private void newAMMinedCommit(String commonRule) {
        String rule_list= System.getenv("SONAR_RULES")!=null? System.getenv("SONAR_RULES"): SoboConstants.RULES_SLOT_1 ;
        Bson userRepoFilter = Filters.and(
                Filters.eq("user", commitAuthor),
                Filters.eq("commit", commitSHA),
                Filters.eq("task", task),
                Filters.eq("rule_list", rule_list),
                Filters.eq("clear", false),
                Filters.eq("mostCommonRule", commonRule)
        );

        MongoIterable<Document> duplicate = soboAM.find(userRepoFilter);
        if(!duplicate.iterator().hasNext()){
            addAMMinedCommit( rule_list, commonRule);}
        else System.out.println("DATA already in the DB");

    }

    private void addAMMinedCommit(String rule_list, String commonRule) {
        soboAM.insertOne(new Document()
                .append("_id", new ObjectId() )
                .append("user", commitAuthor)
                .append("commit",commitSHA)
                .append("task", task)
                .append("rule_list", rule_list)
                .append("clear", false)
                .append("mostCommonRule", commonRule)
                .append("time", timestamp.getTime()));
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

    public void readCommand(ProjectInspector inspector, String user, String task, String slug)  {
        this.task=task;
        this.inspector=inspector;
        this.logger=inspector.getLogger();
        GHIssue mainIssue=getCommandIssue(inspector,user);
        if (mainIssue!=null){
        List<GHIssueComment> commentList = getIssueComments(mainIssue);
        if (!commentList.isEmpty()){
            GHIssueComment comment = getLastCommand(commentList);
            if(comment!=null){
            commands= connectToDB(user, "commands");
            soboCL= connectToDB(user, "soboCL");
            String[] commandLine = comment.getBody().split(System.lineSeparator())[0].split(" ");
            inspector.getLogger().info(Arrays.toString(commandLine));
            if (commandLine.length>0 ){
                String command=commandLine[0].toLowerCase();
                if (command.equals(SoboConstants.HELP)) help(user,  mainIssue);
                else if (command.equals(SoboConstants.STOP)) stop(user,task,mainIssue);
                else if (command.equals(SoboConstants.GO)) go(user,task, mainIssue);
                else if (command.equals(SoboConstants.MORE) ) more( mainIssue , commandLine, slug,user , task);
                else if (command.equals(SoboConstants.RULE)) rule(mainIssue , commandLine,slug, user , task);
                else if (honey_checker(comment.getBody())) honey_answer( mainIssue , commandLine, user , task) ;
                else {
                    cantReadCommand(mainIssue, Arrays.toString(commandLine),user);
                    inspector.getLogger().info("Can't read command");
                }

        }}}}
    }



    public void analyzeCommand(String user, String slug, String task,Logger logger, GHIssueComment comment, GHIssue mainIssue) {
        this.task=task;
        this.logger=logger;
        commands= connectToDB(user, "commands");
        soboCL= connectToDB(user, "soboCL");
        String[] commandLine = comment.getBody().split(System.lineSeparator())[0].split(" ");
        logger.info(Arrays.toString(commandLine));
        if (commandLine.length>0 ){
            String command=commandLine[0].toLowerCase();
            if (command.equals(SoboConstants.HELP)) help(user,  mainIssue);
            else if (command.equals(SoboConstants.STOP)) stop(user,task,mainIssue);
            else if (command.equals(SoboConstants.GO)) go(user,task, mainIssue);
            else if (command.equals(SoboConstants.MORE) ) more( mainIssue , commandLine, slug,user , task);
            else if (command.equals(SoboConstants.RULE)) rule(mainIssue , commandLine,slug, user , task);
            else if (honey_checker(comment.getBody())) honey_answer( mainIssue , commandLine, user , task) ;
            else {
                cantReadCommand(mainIssue, Arrays.toString(commandLine),user);
                logger.info("Can't read command");
            }
        }}






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

    public GHIssue getCommandIssue(String slug, String user, Logger logger) {
        GitHub github = connectWithGH();
        GHIssue commandIssue = null;
        try {
            GHRepository repo = github.getRepository(slug);
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
            logger.info("Not able to get the repo");
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
        if (lastIndex > -1){
        try {
            GHIssueComment comment = comments.get(lastIndex);
            String login = comment.getUser().getLogin();
            String repoName = comment.getParent().getRepository().getName();
            long issueID = comment.getParent().getId();
            String command=comment.getBody().split(" ")[0];
            String body=comment.getBody();
            String user=comment.getUser().getLogin();
            long time = comment.getCreatedAt().getTime();

            if (!login.equals("system-sobo") ){
                newUserCommand(command, body,time,user,repoName,issueID);
                return comment;
            }
        } catch (IOException e) {
            System.out.println("Can´t find comment's User");
        }}
        return null;
    }

    public void newUserCommand(String command, String body, long time, String user, String repoName, long issueID){
        Bson userRepoFilter = Filters.and(
                Filters.eq("command", command),
                Filters.eq("body", body),
                Filters.eq("timestamp", time),
                Filters.eq("user", user),
                Filters.eq("repo", repoName),
                Filters.eq("issue", issueID)
        );
        if (commands==null) commands= connectToDB(user, "commands");


        MongoIterable<Document> duplicate = commands.find(userRepoFilter);
        if(!duplicate.iterator().hasNext()){
            addUserCommand(command, body,time,user,repoName,issueID);
        }

    }



    public void addUserCommand(String command, String body, long time, String user, String repoName, long issueID){
        commands.insertOne(new Document()
                .append("command", command)
                .append("body", body)
                .append("timestamp", time)
                .append("user", user)
                .append("repo", repoName)
                .append("issue", issueID));

    }
    private void cantReadCommand(GHIssue issue, String command, String user){
        try {
            issue.comment("I can't process that command");
            addCLMessage(command, user, false, issue.getNodeId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void cantReadRULECommand(GHIssue issue, String user, String rule){
        try {
            issue.comment("I can't process that command");
            addCLRULEMessage(SoboConstants.RULE, user, false, issue.getNodeId(), rule);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void cantReadMORECommand(GHIssue issue, String user, String commit){
        try {
            issue.comment("I can't process that command");
            addCLMOREMessage(SoboConstants.MORE, user, false, issue.getNodeId(), commit);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addCLMessage(String command, String user, boolean result, String issueID ){
        logger.info("Saving SOBO response");
        soboCL.insertOne(new Document()
                .append("command", command)
                .append("success", result)
                .append("timestamp", timestamp.getTime())
                .append("user", user)
                .append("repo", task)
                .append("issue", issueID));

    }
    public void addCLMOREMessage(String command, String user, boolean result, String issueID, String commit ){
        logger.info("Saving SOBO response");
        soboCL.insertOne(new Document()
                .append("command", command)
                .append("success", result)
                .append("timestamp", timestamp.getTime())
                .append("user", user)
                .append("repo", task)
                .append("issue", issueID)
                .append("commit",commit));

    }
    public void addCLRULEMessage(String command, String user, boolean result, String issueID, String rule ){
        logger.info("Saving SOBO response");
        soboCL.insertOne(new Document()
                .append("command", command)
                .append("success", result)
                .append("timestamp", timestamp.getTime())
                .append("user", user)
                .append("repo", task)
                .append("issue", issueID)
                .append("rule",rule));

    }

    private static String getEnvOrDefault(String name, String dfault) {
        String env = System.getenv(name);
        if (env == null || env.equals(""))
            return dfault;

        return env;
    }


    private void help(String user, GHIssue mainIssue){
        logger.info("Command : "+ SoboConstants.HELP);
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
            addCLMessage(SoboConstants.HELP, user,true,mainIssue.getNodeId());
        } catch (IOException e) {
            cantReadCommand(mainIssue, SoboConstants.HELP, user);
            logger.info("Can't comment issue");
        }

    }
    private void stop(String user, String task, GHIssue mainIssue){
        this.users=connectToDB(user, UsersColl);
        logger.info("Command : "+ SoboConstants.STOP);
        users.findOneAndUpdate(
                Filters.eq("ghID", user),
                Updates.set(task, false));
        try {
            mainIssue.comment("I will not generate automatic responses for this repo, but remember you can activate me again whenever you want using \\go :) ");
            addCLMessage(SoboConstants.STOP, user,true,mainIssue.getNodeId());
        } catch (IOException e) {
            cantReadCommand(mainIssue,SoboConstants.STOP, user);
            System.out.println("Can't send comment message");
        }

    }
    private void go(String user, String task, GHIssue mainIssue){
        this.users=connectToDB(user, UsersColl);
        logger.info("Command : "+ SoboConstants.GO);
        users.findOneAndUpdate(
                Filters.eq("ghID", user),
                Updates.set(task, true));
        try {
            logger.info("Sending message");
            mainIssue.comment("Hello again! ");
            addCLMessage(SoboConstants.GO, user,true,mainIssue.getNodeId());
        } catch (IOException e) {
            cantReadCommand(mainIssue, SoboConstants.GO, user);
            System.out.println("Can't send comment message");
        }
    }




    private void more(GHIssue mainIssue, String[] commandBody,String slug, String user, String task){
        StringBuilder comment = new StringBuilder();
        String commit= "NOT-VALID";
        try {
            minedViolations= connectToDB(user, mineViolationColl);

            if (commandBody.length >1) {
                commit= commandBody[1];
                logger.info("Command : " + SoboConstants.MORE + " on commit : " + commit);
                Bson userRepoFilter = Filters.and(
                        Filters.eq("commit", commit),
                        Filters.eq("user", user),
                        Filters.eq("task", task)
                );
                MongoCursor<Document> violations = minedViolations.find(userRepoFilter).iterator();

                if (violations.hasNext()) {
                    comment.append("|line | FilePath| Rule | \n");
                    comment.append("|--|--|--| \n");
                    while (violations.hasNext()) {
                        Document next = violations.next();
                        String fPath = next.get("filePath").toString().replace("\\", "/");
                        String fileClickeable = "[" + next.get("filePath") + "](https://gits-15.sys.kth.se/";
                        if (fPath.contains("/")) {
                            fileClickeable += slug + "/tree/" + commit + "/" + fPath + ")";
                        } else fileClickeable += slug+ "/blob/" + commit + "/" + fPath + ")";
                        comment.append("|" + next.get("line") + " | " + fileClickeable + " |" + next.get("rule") + "\n");
                    }
                } else comment.append("I don't have data about violations on commit: "+ commit);

            }else comment.append("You need to give a commit SHA so I can run the query on my database");

            mainIssue.comment(String.valueOf(comment));
            addCLMOREMessage(SoboConstants.MORE, user, true, mainIssue.getNodeId(), commit);
            } catch (Exception e) {
            cantReadMORECommand(mainIssue, user, commit);
            logger.info("Error while adding a comment");
        }



    }
    private void rule(GHIssue mainIssue, String[] commandBody, String user, String task, String slug){
        minedViolations= connectToDB(user, mineViolationColl);
        String rule="NOT-VALID";
        try {
        StringBuilder comment = new StringBuilder();
        if (commandBody.length>1) {
            rule=commandBody[1].toUpperCase();
            if  (Arrays.asList(SoboConstants.SOBO_RULES).contains(rule)) {
            logger.info("Command : " + SoboConstants.RULE + " on rule : " + rule);
            Bson userRepoFilter = Filters.and(
                    Filters.eq("user", user),
                    Filters.eq("task", task),
                    Filters.eq("rule", rule)
            );
            MongoCursor<Document> getLastCommit = minedViolations.find(userRepoFilter).sort(Sorts.descending("time")).iterator();
            String commit = "";
            if (getLastCommit.hasNext()) {
                Document next = getLastCommit.next();
                commit = next.get("commit").toString();
            }
            Bson ruleCommitFilter = Filters.and(
                    Filters.eq("commit", commit),
                    Filters.eq("user", user),
                    Filters.eq("task", task),
                    Filters.eq("rule", rule)
            );
            MongoCursor<Document> violations =  minedViolations.find(ruleCommitFilter).iterator();
            if (violations.hasNext()) {
                comment.append("|line | FilePath| Rule | \n");
                comment.append("|--|--|--| \n");
                while (violations.hasNext()) {
                    Document next = violations.next();
                    String fPath = next.get("filePath").toString().replace("\\", "/");
                    String fileClickeable = "[" + next.get("filePath") + "](https://gits-15.sys.kth.se/";
                    if (fPath.contains("/")) {
                        fileClickeable += slug + "/tree/main/" + fPath + ")";
                    } else fileClickeable += slug + "/blob/main/" + fPath + ")";
                    comment.append("|" + next.get("line") + " | " + fileClickeable + " |" + next.get("rule") + "\n");
                }
            } else comment.append("I don't have data about rule " + rule + " on your last commit");

        }
            else {
                comment.append("You need to give a valid rule, so I can run the query on my database \n");
                comment.append("Options: ").append(getEnvOrDefault("SONAR_RULES", SoboConstants.RULES_SLOT_1));

            }
        }
        else {
            comment.append("You need to give a valid rule, so I can run the query on my database \n");
            comment.append("Options: ").append(getEnvOrDefault("SONAR_RULES", SoboConstants.RULES_SLOT_1));

        }

            mainIssue.comment(String.valueOf(comment));
            addCLRULEMessage(SoboConstants.RULE, user, true, mainIssue.getNodeId(), rule);
        } catch (Exception e) {
            cantReadRULECommand(mainIssue, user, rule);
            logger.info("Error while adding a comment");
        }


    }
    private boolean honey_checker(String command) {
        logger.info("Command HONEY: "+ command);
        for (String honey_com : SoboConstants.HONEY_COMMANDS ){
            if (command.contains(honey_com)) return true;
        }
        return false;
    }

    private void honey_answer(GHIssue mainIssue, String[] command, String user, String task) {
        String actualPath= System.getProperty("user.dir");
        String templates= getPath("templates", actualPath);
        Path pathHeader = Paths.get(templates + "/honey.md");
        try {
            StringBuilder issueBody = new StringBuilder();
            Files.lines(pathHeader, StandardCharsets.UTF_8).forEach(line -> {
                // replace all instances of {@task} with the student's name
                line.replace("{task}", task);
                issueBody.append(line).append(System.lineSeparator());
            });
            mainIssue.comment(issueBody.toString());
        } catch (IOException e) {
            cantReadCommand(mainIssue, Arrays.toString(command),user);
            logger.info("Can't comment issue");
        }
    }

    /**
     * Mthod to check if the user belongs to the organization
     * @param userName user
     * @return true if is a valid user
     */
    public boolean checkUser(String userName) {
        MongoCollection<Document> collection=connectToDB(userName, UsersColl);
        Bson userFilter = Filters.and(
                Filters.eq("ghID", userName)
        );
        MongoIterable<Document> duplicate = collection.find(userFilter);
        return duplicate.iterator().hasNext();
    }

    public boolean checkUserRepo(String userName, String task) {
        MongoCollection<Document> collection=connectToDB(userName, UsersColl);

        Bson userTaskFilter = Filters.and(
                Filters.eq("ghID", userName),
                Filters.eq(task, true)
        );
        MongoIterable<Document> duplicate = collection.find(userTaskFilter);
        return duplicate.iterator().hasNext();
    }
}
