package fr.inria.spirals.repairnator.realtime.utils;


/**
 * Single-hunk, single-line-change patch filter.
 * */
public class PatchFilter {
    
    enum State {
        ENTRY,
        HEADER,
        ENTRY_CONTEXT,
        EXIT_CONTEXT,
        REMOVE,
        ADD
    }
    
    public class PatchLines {
        public PatchLines(String removed, String added) {
            this.removed = removed;
            this.added = added;
        }
        
        public final String removed;
        public final String added;
    }
    
    public boolean test(String patch) {

        State state = State.ENTRY;
        String[] lines = patch.split("\n");
        
        for(String line : lines) {
            switch(state) {
            
            case ENTRY:
                //assume first line hunk header
                state = State.HEADER;
                break;
            
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
            
            case ADD:{
                char first = line.charAt(0);
                if(first == ' ') {
                    state = State.EXIT_CONTEXT;
                }else{
                    return false;
                }
            }break;
            
            case EXIT_CONTEXT:{
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
    
    public PatchLines parse(String patch) {
        String[] lines = patch.split("\n");
        
        String removed= "";
        String added = "";
        
        for(String line : lines) {
            char first = line.charAt(0);
            if(first == '-') removed = line.substring(1).trim();
            if(first == '+') added = line.substring(1).trim();
        }
        
        return new PatchLines(removed, added);
    }
}
