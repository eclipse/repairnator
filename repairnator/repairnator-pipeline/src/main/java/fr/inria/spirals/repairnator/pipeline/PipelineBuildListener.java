package fr.inria.spirals.repairnator.pipeline;

import javax.jms.MessageListener;

/**
 * This class listen for build ids from ActiveMQ queue and run the pipeline with it.
 */
public class PipelineBuildListener implements MessageListener {}
