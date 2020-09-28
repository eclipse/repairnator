package fr.inria.spirals.repairnator.realtime.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.*;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHCommit;

/**
 * Single-line-change filter.
 * */
public class PatchFilter {
    
    static final String HUNK_HEADER_REGEX = "^@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@ ?.*$";
    static final String SPLIT_BY_HUNKS_REGEX = "(?<=\\n)(?=@@ -\\d+,\\d+ \\+\\d+,\\d+ @@ ?.*\\n)";
    
    enum State {
        ENTRY,
        HEADER,
        ENTRY_CONTEXT,
        EXIT_CONTEXT,
        REMOVE,
        ADD
    }
    
    public class HunkLines {
        public HunkLines(String removed, String added) {
            this.removed = removed;
            this.added = added;
        }
        
        public final String removed;
        public final String added;
    }
    
    String[] splitByLines(String hunk) {
        return hunk.split("\n");
    }
    
    boolean isRegularHunkHeader(String hunkHeader) {

        Pattern p = Pattern.compile(HUNK_HEADER_REGEX);
        Matcher m = p.matcher(hunkHeader);
        
        if(m.find()) {
            if(m.group(1).equals(m.group(3)) && m.group(2).equals(m.group(4))) {
                return true;
            }
        }
        return false;
    }
    
    int getHunkLine(String hunkHeader) {

        Pattern p = Pattern.compile(HUNK_HEADER_REGEX);
        Matcher m = p.matcher(hunkHeader);
        
        if(m.find()) {
            return Integer.parseInt(m.group(3));   
        }
        return -1;
        
    }
    
    boolean test(String[] hunkLines) {

        State state = State.ENTRY;
        
        for(String line : hunkLines) {
            switch(state) {
            
                //assuming first line is hunk header.
                case ENTRY:{
                    state = State.HEADER;
                    break;
                }

                case HEADER:{
                    char first = line.charAt(0);
                    if(first == '-') {
                        state = State.REMOVE;
                    } else if(first == ' ') {
                        state = State.ENTRY_CONTEXT;
                    } else if(first == '+') {
                        return false;
                    }

                }break;

                case ENTRY_CONTEXT:{
                    char first = line.charAt(0);
                    if(first == '-') {
                        state = State.REMOVE;
                    } else if(first == ' ') {
                        state = State.ENTRY_CONTEXT;
                    } else {
                        return false;
                    }
                }break;

                case REMOVE:{
                    char first = line.charAt(0);
                    if(first == '+') {
                        state = State.ADD;
                    }else{
                        return false;
                    }
                }break;

                case ADD:
                case EXIT_CONTEXT: {
                    char first = line.charAt(0);
                    if(first == ' ') {
                        state = State.EXIT_CONTEXT;
                    }else{
                        return false;
                    }
                }break;
            }
        }
        
        
        return state == State.EXIT_CONTEXT || state == State.ADD;
    }
    
    public ArrayList<SequencerCollectorHunk> getHunks(ArrayList<SequencerCollectorPatch> patches, boolean singleHunk, int hunkDistance){
        
        ArrayList<SequencerCollectorHunk> ret = new ArrayList<SequencerCollectorHunk>();

        for(SequencerCollectorPatch patch : patches) {
            String[] hunks = patch.getContent().split(SPLIT_BY_HUNKS_REGEX);
            
            ArrayList<Boolean> oneLineHunks  = new ArrayList<Boolean>();
            ArrayList<Integer> linePositions  = new ArrayList<Integer>();
            
            if(singleHunk && hunks.length > 1) continue;
            
            for(String hunk: hunks) {
                String[] lines = splitByLines(hunk);
                
                linePositions.add(getHunkLine(lines[0]));
                oneLineHunks.add(isRegularHunkHeader(lines[0]) && test(lines));
            } 
            
            for(int i = 0; i < oneLineHunks.size() - 1; ++i) {
                boolean isFarEnough = oneLineHunks.get(i) && ( linePositions.get(i + 1) - linePositions.get(i) > hunkDistance);
                oneLineHunks.set(i, isFarEnough);
            }
            
            for(int i = oneLineHunks.size() - 1; i > 0; --i) {
                boolean isFarEnough = oneLineHunks.get(i) && ( linePositions.get(i) - linePositions.get(i - 1) > hunkDistance);
                oneLineHunks.set(i, isFarEnough);
            }
            
            for(int i = 0; i < oneLineHunks.size(); ++i) {
                if(oneLineHunks.get(i)){
                    ret.add( new SequencerCollectorHunk(linePositions.get(i), patch.getFile(), hunks[i]));
                }
            }
            
        }
        return ret;
    }
    
        
    
    public HunkLines parse(String hunk) {
        String[] lines = splitByLines(hunk);
        
        String removed= "";
        String added = "";
        
        for(String line : lines) {
            char first = line.charAt(0);
            if(first == '-') removed = line.substring(1).trim();
            if(first == '+') added = line.substring(1).trim();
        }
        
        return new HunkLines(removed, added);
    }
    

    private ArrayList<SequencerCollectorPatch> getCommitPatches(GHCommit commit, boolean filterMultiFile) throws IOException{
        ArrayList<SequencerCollectorPatch> ret = new ArrayList<>();
        
        List<GHCommit.File> files = commit.getFiles(); 
        List<GHCommit.File> javaFiles = new ArrayList<>();
        
        for(GHCommit.File f : files) {
            if(f.getFileName().endsWith(".java")) {
                javaFiles.add(f);
            }
        }

        if(filterMultiFile && javaFiles.size() != 1) {
            return ret;
        }

        for(GHCommit.File f : javaFiles) {
            if(f.getPatch() != null) { //sometimes this call returns null
                String fullFilename = f.getFileName();
                ret.add( new SequencerCollectorPatch(fullFilename.substring(fullFilename.lastIndexOf("/") + 1), f.getPatch()) );
            }
        }

        return ret;
    }

    /**
     * Returns a list of patches.
     *
     * if filterMultiFile is true, it will filter out multi-file commits
     * */
    public ArrayList<SequencerCollectorPatch> getCommitPatches(GHCommit commit, boolean filterMultiFile, int contextSize) throws IOException{

        if(contextSize == 3) {
            // return contents of commit patches, skip diff calculation
            return getCommitPatches(commit, filterMultiFile);
        }

        ArrayList<SequencerCollectorPatch> ret = new ArrayList<>();

        List<GHCommit.File> files = commit.getFiles();
        List<GHCommit.File> javaFiles = new ArrayList<>();

        for(GHCommit.File f : files) {
            if(f.getFileName().endsWith(".java")) {
                javaFiles.add(f);
            }
        }

        if(filterMultiFile && javaFiles.size() != 1) {
            return ret;
        }

        for(GHCommit.File f : javaFiles) {
            if(f.getPatch() != null) { //sometimes this call returns null
                String fullFilename = f.getFileName();

                // don't use github client since we may need to change target urls
                // to a mirror in case rate limits are exceeded

                // read from url, //raw.githubusercontent.com/owner/repo/sha/path
                String fileURL = "https://raw.githubusercontent.com/" + commit.getOwner().getFullName() + "/"
                        + commit.getSHA1() + "/" + fullFilename;

                // read from url, //raw.githubusercontent.com/owner/repo/parentsha/path
                String parentFileURL = "https://raw.githubusercontent.com/" + commit.getOwner().getFullName() + "/"
                        + commit.getParents().get(0).getSHA1() + "/" + fullFilename;

                try {

                    String changes = readFromURL(fileURL);
                    String past = readFromURL(parentFileURL);

                    List<String> changesAsList = Arrays.asList(changes.split("\n").clone());
                    List<String> pastAsList = Arrays.asList(past.split("\n").clone());

                    Patch<String> diff = DiffUtils.diff(pastAsList, changesAsList);

                    List<String> uDiff = UnifiedDiffUtils.generateUnifiedDiff(fullFilename, fullFilename, pastAsList, diff, contextSize);
                    String patch = String.join("\n", uDiff);

                    ret.add( new SequencerCollectorPatch(fullFilename.substring(fullFilename.lastIndexOf("/") + 1), patch) );

                }catch(Exception e){
                    System.out.println("ERROR IN SEQUENCER COLLECTOR: " + e);
                }

            }
        }

        return ret;
    }

    String readFromURL(String urlStr){
        String data;
        try {
            URL u = new URL(urlStr);
            data = IOUtils.toString(u,"UTF-8");
        }catch (Exception e){
            throw new RuntimeException("Error while reading file from URL:" + urlStr);
        }
        return data;
    }
}
