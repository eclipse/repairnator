package fr.inria.spirals.repairnator.realtime.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.*;
import java.util.stream.Collectors;

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
        
        ArrayList<SequencerCollectorHunk> ret = new ArrayList<>();

        for(SequencerCollectorPatch patch : patches) {
            String[] split = patch.getContent().split(SPLIT_BY_HUNKS_REGEX);
            String[] hunks = Arrays.copyOfRange(split, 1, split.length); //drop file header
            
            ArrayList<Boolean> oneLineHunks  = new ArrayList<>();
            ArrayList<Integer> linePositions  = new ArrayList<>();
            
            if(singleHunk && hunks.length != 1) continue;
            
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

    /**
     * Returns a list of patches.
     *
     * if filterMultiFile is true, it will filter out multi-file commits
     * */
    public ArrayList<SequencerCollectorPatch>
    getCommitPatches(GHCommit commit, boolean filterMultiFile, int contextSize, Map<String, String> rawFilesStore) throws IOException{

        ArrayList<SequencerCollectorPatch> ret = new ArrayList<>();

        List<GHCommit.File> javaFiles = commit
                .getFiles().stream()
                .filter(file -> file.getFileName().endsWith(".java"))
                .filter(file -> file.getLinesAdded() != 0) //filter deletion only changes
                .filter(file -> file.getLinesDeleted() != 0) // filter addition only changes
                .collect(Collectors.toList());

        if(filterMultiFile && javaFiles.size() != 1) {
            return ret;
        }

        javaFiles.forEach(f -> {
            if(f.getPatch() != null) { //sometimes this call returns null
                String fullFilename = f.getFileName();
                try {
                    // don't use github client since we may need to change target urls
                    // to a mirror in case rate limits are exceeded

                    // read from url, //raw.githubusercontent.com/{owner}/{repo}/{sha}/{path}
                    String fileURL = RawURLGenerator.Generate(commit.getOwner().getFullName(), commit.getSHA1(), fullFilename);

                    // read from url, //raw.githubusercontent.com/{owner}/{repo}/{parent_sha}/{path}
                    String parentFilename = fullFilename;
                    String previousFilename = f.getPreviousFilename();

                    //if name change occurred -> get correct parent file
                    if(previousFilename != null && !previousFilename.isEmpty() && !previousFilename.equals(fullFilename)){
                        parentFilename = previousFilename;
                    }

                    String parentFileURL = RawURLGenerator.Generate(
                            commit.getOwner().getFullName(),
                            commit.getParents().get(0).getSHA1(),
                            parentFilename
                    );

                    String changes = readFromURL(fileURL);
                    String past = readFromURL(parentFileURL);

                    rawFilesStore.put(fullFilename, past);

                    List<String> changesAsList = Arrays.asList(changes.split("\n").clone());
                    List<String> pastAsList = Arrays.asList(past.split("\n").clone());

                    Patch<String> diff = DiffUtils.diff(pastAsList, changesAsList);

                    List<String> uDiff = UnifiedDiffUtils.generateUnifiedDiff(fullFilename, fullFilename, pastAsList, diff, contextSize);
                    String patch = String.join("\n", uDiff);

                    ret.add( new SequencerCollectorPatch(fullFilename, patch));

                }catch(Exception e){
                    System.out.println("Exception while getting raw files, skipping patch: " + e);
                }
            }
        });


        return ret;
    }

    /**
     * Method to get a file from a GitHub repository using its URL path
     * @param urlStr path of the file
     * @return the data in the file as plain text
     */
    String readFromURL(String urlStr){
        String data;
        try {
            URL u = new URL(urlStr);
            data = IOUtils.toString(u,"UTF-8");
        }catch (Exception e){

            throw new RuntimeException("Error while reading file from URL:" + urlStr + " -- type: " + e.getClass().getName() +" --message "+e.getMessage()+" --cause "+ e.getCause().toString()));
        }
        return data;
    }
}
