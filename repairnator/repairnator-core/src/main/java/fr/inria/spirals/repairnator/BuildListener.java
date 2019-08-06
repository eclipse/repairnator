package fr.inria.spirals.repairnator;

import javax.jms.MessageListener;
/**
 * This class fetch build ids from ActiveMQ queue and run the pipeline with it.
 */
public interface BuildListener extends MessageListener {}
