package fr.inria.spirals.jtravis.parsers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 03/01/2017.
 */
public class TravisFold {
    private String foldName;
    private List<String> content;
    private String wholeContent;

    public TravisFold(String foldName) {
        this.foldName = foldName;
        this.content = new ArrayList<String>();
        this.wholeContent = "";
    }

    public void addContent(String line) {
        this.content.add(line);
        this.wholeContent += line+"\n";
    }

    public List<String> getContent() {
        return this.content;
    }

    public String getWholeContent() {
        return this.wholeContent;
    }
}
