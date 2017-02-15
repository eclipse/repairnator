package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;

public class GatherTestInformation4Bears extends GatherTestInformation {

	public GatherTestInformation4Bears(ProjectInspector inspector) {
		super(inspector);
	}

	@Override
    protected void businessExecute() {
		super.businessExecute();
		
    	if (!((ProjectInspector4Bears)this.inspector).isAboutAPreviousBuild()) {
    		this.shouldStop = !this.shouldStop;
    	} else {
    		if (this.getState() == ProjectState.HASTESTFAILURE) {
    	    	// So, 1) the current passing build can be reproduced and 2) its previous build is a failing build with failing tests and it can also be reproduced
    	    	this.setState(ProjectState.FIXERBUILD);
    	    	this.shouldStop = false;
    	    } else {
    	    	this.shouldStop = true;
    	    }
    	}
	}
}