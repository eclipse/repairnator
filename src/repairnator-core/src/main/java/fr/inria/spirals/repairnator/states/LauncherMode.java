package fr.inria.spirals.repairnator.states;

/**
 * Created by urli on 25/01/2017.
 * 
 * Launcher modes that can be used to run Repairnator.
 */
public enum LauncherMode {
    
	/**
	 * REPAIR: This mode allows to use Repairnator to analyze bugs present in a Continuous Integration build.
	 */
	REPAIR,
    
	/**
	 * BEARS: This mode allows to use Repairnator to analyze pairs of bugs and human-produced patches.
	 */
	BEARS,
	
	/**
	 * CHECKSTYLE: This mode allows to use Repairnator to analyze build failing because of checkstyle.
	 */
    CHECKSTYLE,
    
    /**
     * GIT_REPOSITORY: This mode allows to use Repairnator to analyze bugs present in a Git repository.
     */
    GIT_REPOSITORY
}
