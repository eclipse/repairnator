package fr.inria.spirals.repairnator.realtime.utils;

public class SOBOUtils {
    /**
     *
     * @param repoName the slug of the student repository following the sintax inda-{year}/{user}-task-{n}
     * @return userName of the owner of the repo
     */
    public static String getUserName(String repoName){
        char[] chars = repoName.toCharArray();
        String user="";
        int index = repoName.indexOf("inda-");
        if (index!=-1){
            for(int i =index+8;i< chars.length;i++){
                if(chars[i]=='-'){
                    return user;
                }
                user+=chars[i];

            }
            return user;
        }
        index = repoName.indexOf('/');

        for(int i =0;i<index;i++){
            user+=chars[i];
        }
        return user;

    }

    public static String getTask(String repoName){
        StringBuilder task= new StringBuilder();
        char[] chars = repoName.toCharArray();

        // iterate over `char[]` array using enhanced for-loop

        int index = repoName.indexOf("task-");
        if (index==-1){
            return task.toString();
        }
        for(int i =index;i< chars.length;i++){
            task.append(chars[i]);
        }
        return task.toString();

    }
}
