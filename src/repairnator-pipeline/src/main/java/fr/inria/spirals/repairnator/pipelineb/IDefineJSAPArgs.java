package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;

public interface IDefineJSAPArgs {

	JSAP defineArgs() throws JSAPException;

}