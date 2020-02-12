package fr.inria.spirals.repairnator.realtime.utils;

public class SequencerCollectorPatch {
    public SequencerCollectorPatch(String file, String content) {
        this.file = file;
        this.content = content;
    }
    
    final String file;
    final String content;
    
    public String getContent() {
        return content;
    }
    
    public String getFile() {
        return file;
    }
}
