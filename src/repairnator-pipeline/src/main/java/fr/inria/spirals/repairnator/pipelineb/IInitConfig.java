package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.JSAP;

public interface IInitConfig {

	void initConfigWithJSAP(JSAP jsap,String[] inputArgs);
}