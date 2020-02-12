package fr.inria.spirals.repairnator.realtime.utils;

public class SequencerCollectorHunk {
    
        public SequencerCollectorHunk(int line, String file, String content) {
            this.line = line;
            this.file = file;
            this.content = content;
        }
        
        final int line;
        final String file;
        final String content;
        
        public String getContent() {
            return content;
        }
        
        public String getFile() {
            return file;
        }
        
        public int getLine() {
            return line;
        }
}
