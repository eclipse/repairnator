package fr.inria.spirals.repairnator.process.step.logParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maven/Java log parser
 * Reproduced from tdurieux/Travis-Listener implementation
 *
 * Author: Javier Ron
 */
public class LogParser {

    List<Independent> independents;
    List<Group> groups;
    List<Element> tests;
    List<Element> errors;

    String inGroup;
    Element currentElement;


    public LogParser(){
        independents = new ArrayList<>();
        independents.add(new Independent(Pattern.compile("\\[javac] (?<file>[^:]+):(?<line>[0-9]+): error: (?<message>.*)"), "test", "Test"));
        independents.add(new Independent(Pattern.compile("\\[ERROR] (?<file>[^:]+):\\[(?<line>[0-9]+)(,(?<column>[0-9]+))?] (?<message>\\((.+)\\) (.+))\\."), "test", "Test"));

        groups = new ArrayList<>();


        groups.add(new Group("checkstyle", "Checkstyle", "Chore",
                Pattern.compile("\\[INFO] There (is|are) (.+) errors? reported by Checkstyle .+ with (.+) ruleset\\."),
                Pattern.compile("\\[INFO] -+"),
                Pattern.compile("\\[ERROR] (?<file>[^:]+):\\[(?<line>[0-9]+)(,(?<column>[0-9]+))?] (?<message>.+)\\.")
        ));

        groups.add(new Group("compile", "Compilation", "Compilation",
                Pattern.compile(".*\\[ERROR] COMPILATION ERROR.*"),
                Pattern.compile(".*\\[INFO] ([0-9]+) errors?"),
                Pattern.compile("\\[ERROR] (?<file>[^:]+):\\[(?<line>[0-9]+)(,(?<column>[0-9]+))?] (?<message>.+)")
        ));

        groups.add(new Group("compile", "Compilation", "Compilation",
                Pattern.compile(".*\\[ERROR] COMPILATION ERROR.*"),
                Pattern.compile(".*location: +(.+)"),
                Pattern.compile(".*\\[ERROR] (?<file>[^:]+):\\[(?<line>[0-9]+)(,(?<column>[0-9]+))?] (?<message>.+)")
        ));

        groups.add(new Group("compile", "Compilation", "Compilation",
                Pattern.compile(".*\\[ERROR] COMPILATION ERROR.*"),
                Pattern.compile(".*location: +(.+)"),
                Pattern.compile("(?<file>[^:]+):\\[(?<line>[0-9]+)(,(?<column>[0-9]+))?] error: (?<message>.+)")
        ));

        groups.add(new Group("compile", "Compilation", "Compilation",
                Pattern.compile("\\[ERROR] COMPILATION ERROR.*"),
                Pattern.compile(".*location: +(.+)"),
                Pattern.compile("(?<file>.+):(?<line>[0-9]+): error: (?<message>.+)")
        ));



        inGroup = null;
        currentElement = null;
        tests = new ArrayList<>();
        errors = new ArrayList<>();
    }

    public List<Element> getErrors() {
        return errors;
    }

    public List<Element> getTests() {
        return tests;
    }

    public void parse(List<String> log){
        for(String line : log){
            parseLine(line);
        }
    }

    public void parse(String log){
        String[] lines = log.split("\n");
        parse(Arrays.asList(lines));
    }

    public void parseLine(String line){
        for (Group group : groups) {
            if (this.inGroup != null && !group.name.equals(this.inGroup)) {
                continue;
            }

            if (this.inGroup == null) {
                LogMatcher matcher = new LogMatcher(group.start.matcher(line));
                if (matcher.matches()) {
                    this.inGroup = group.name;
                    if (group.type.equals("test")) {
                        this.currentElement = new Element()
                                .put("name", matcher.group("name"))
                                .put("class", matcher.group("class"))
                                .put("body", "")
                                .put("nbTest", 1)
                                .put("nbFailure", 0)
                                .put("nbError", 0)
                                .put("nbSkip", 0)
                                .put("time", 0f);
                        this.tests.add(this.currentElement);
                    }
                    return;
                }
            } else {
                LogMatcher matcher = new LogMatcher(group.end.matcher(line));
                if (matcher.matches()) {
                    if (this.currentElement != null && matcher.groupCount() > 0 && matcher.group("nbTest") != null) {
                        this.currentElement.put("nbTest", parseIntOrNull(matcher.group("nbTest")));
                        this.currentElement.put("nbFailure", parseIntOrNull(matcher.group("failure")));
                        this.currentElement.put("nbError", parseIntOrNull(matcher.group("error")));
                        this.currentElement.put("nbSkipped", parseIntOrNull(matcher.group("skipped")));
                        this.currentElement.put("time", parseFloatOrNull(matcher.group("time")));
                    }
                    return;
                }
                LogMatcher matcher2 = new LogMatcher(group.element.matcher(line));
                if (matcher2.matches()) {
                    if (matcher2.group("allLine") != null) {
                        //void
                    } else {
                        Element output = new Element()
                                .put("type", group.type)
                                .put("file", matcher2.group("file"))
                                .put("line", parseIntOrNull(matcher2.group("line")))
                                .put("column", parseIntOrNull(matcher2.group("column")))
                                .put("message", matcher2.group("message"))
                                .put("id", matcher2.group("id"));

                        this.errors.add(output);
                    }
                    return;
                }
            }
        }

        for (Independent independent : independents) {
            LogMatcher matcher = new LogMatcher(independent.element.matcher(line));

            if(matcher.matches()){
                Element output = new Element()
                        .put("type", independent.type)
                        .put("name", matcher.group("name"))
                        .put("status", matcher.group("status"))
                        .put("class", matcher.group("class"))
                        .put("nbTest", parseIntOrNull(matcher.group("nbTest")))
                        .put("failed", parseIntOrNull(matcher.group("failed")))
                        .put("skipped", parseIntOrNull(matcher.group("skipped")))
                        .put("message", matcher.group("message"))
                        .put("file", matcher.group("file"))
                        .put("line", parseIntOrNull(matcher.group("line")))
                        .put("column", parseIntOrNull(matcher.group("column")))
                        .put("group", matcher.group("group"))
                        .put("artifact", matcher.group("artifact"))
                        .put("version", matcher.group("version"))
                        .put("artifact", matcher.group("artifact"))
                        .put("rule", matcher.group("rule"))
                        .put("priority", matcher.group("priority"))
                        .put("failure", matcher.group("failure"))
                        .put("error", parseIntOrNull(matcher.group("error")))
                        .put("time", parseFloatOrNull(matcher.group("time")));
                if(independent.type.equals("test")){
                    output.remove("type");
                    if(output.get("status") != null){
                        //log
                    }
                    if(output.get("nbTest") == null){
                        output.put("nbTest", 1);
                    }
                    this.tests.add(output);
                } else {
                    this.errors.add(output);
                }
                return;
            }
        }
    }

    Integer parseIntOrNull(String s){
        if(s == null) return null;
        return Integer.parseInt(s);
    }

    Float parseFloatOrNull(String s){
        if(s == null) return null;
        return Float.parseFloat(s);
    }


    class Independent {
        Pattern element;
        String type;
        String failureGroup;

        Independent(Pattern element, String type, String failureGroup){
            this.element = element;
            this.type = type;
            this.failureGroup = failureGroup;
        }
    }

    class Group {
        String name;
        String type;
        String failureGroup;
        Pattern start;
        Pattern end;
        Pattern element;

        Group(String name, String type, String failureGroup, Pattern start, Pattern end, Pattern element){
            this.name = name;
            this.type = type;
            this.failureGroup = failureGroup;
            this.start = start;
            this.end = end;
            this.element = element;
        }
    }

    class LogMatcher {

        Matcher matcher;

        LogMatcher(Matcher m){
            this.matcher = m;
        }

        String group(String name){
            try{
                return matcher.group(name);
            } catch (IllegalArgumentException e){
                return null;
            }
        }

        int groupCount(){
            return matcher.groupCount();
        }

        Boolean matches(){
            return matcher.matches();
        }
    }


}
