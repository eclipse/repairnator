package fr.inria.spirals.repairnator.pipelineb;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;

public interface IDefineJSAPArgs {

	JSAP defineArgs() throws JSAPException;

}