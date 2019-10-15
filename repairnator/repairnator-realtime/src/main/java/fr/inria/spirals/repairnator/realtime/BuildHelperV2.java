package fr.inria.spirals.repairnator.realtime;

import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import fr.inria.jtravis.JTravis;
import fr.inria.jtravis.TravisConstants;
import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.jtravis.helpers.BuildHelper;


// The motivation of this class is that currently the Travis API is returning inconsistent
// date formats when requesting a build. Therefore this helper and corresponding entity
// excludes all attributes that are not needed for the current purpose.
public class BuildHelperV2 extends BuildHelper {

	public BuildHelperV2(JTravis jtravis) {
		super(jtravis);
	}
	
	
	public Optional<BuildV2> fromIdV2(long id) {
		 Properties properties = new Properties();
	        properties.put("include","job.config");

	        return super.getEntityFromUri(BuildV2.class, Arrays.asList(TravisConstants.BUILD_ENDPOINT, String.valueOf(id)), properties);
	    
	}
	
}
