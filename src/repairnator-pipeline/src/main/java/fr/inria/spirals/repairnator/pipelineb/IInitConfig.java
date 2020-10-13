package fr.inria.spirals.repairnator.pipelineb;

import com.martiansoftware.jsap.JSAP;

public interface IInitConfig {

	void initConfigWithJSAP(JSAP jsap,String[] inputArgs);
}