package fr.inria.spirals.jtravis.parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Created by urli on 03/01/2017.
 */
public class TravisFold {
    private String foldName;
    private List<String> content;

    public TravisFold(String foldName) {
        this.foldName = foldName;
        this.content = new ArrayList<String>();
    }

    public void addContent(String line) {
        this.content.add(line);
    }

    public List<String> getContent() {
        return this.content;
    }

}
