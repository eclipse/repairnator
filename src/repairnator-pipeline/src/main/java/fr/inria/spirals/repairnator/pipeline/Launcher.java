package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;

import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

import java.util.HashSet;
import java.util.Set;

public class Launcher implements LauncherAPI {
    private static LauncherAPI launcher;

    public Launcher() {}

    public Launcher(String[] args) throws JSAPException{
        init(args);
    }

    public static void init(String[] args) throws JSAPException{
        launcher = new BranchLauncher(args);
    }

    public static void main(String[] args) throws JSAPException{
        launcher = new BranchLauncher(args);
        launcher.launch();
    }

    private static String[] removeDuplicatesInArray(String[] arr) {
        Set<String> set = new HashSet<String>();
        for(int i = 0; i < arr.length; i++){
          set.add(arr[i]);
        }
        return set.stream().toArray(String[]::new);
    } 

    @Override
    public boolean mainProcess() {
        return launcher.mainProcess();
    }

    @Override
    public void launch() {
        this.launcher.launch();
    }

    @Override
    public JSAP defineArgs() throws JSAPException{
        return launcher.defineArgs();
    }

    @Override
    public RepairnatorConfig getConfig() {
        return launcher.getConfig();
    }

    @Override
    public ProjectInspector getInspector() {
        return launcher.getInspector();
    }

    @Override
    public void setPatchNotifier(PatchNotifier patchNotifier) {
        launcher.setPatchNotifier(patchNotifier);
    }
}