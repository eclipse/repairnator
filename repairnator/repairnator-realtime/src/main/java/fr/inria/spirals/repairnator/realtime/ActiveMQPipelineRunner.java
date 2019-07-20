package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.dockerpool.AbstractPoolManager;

/**
 * This class will take the builds qualified by the inspectBuild
 * and submit to an ActiveMQ queue for the repairnator-worker
 * to run on Kubernetes.
 */
public class ActiveMQPipelineRunner extends AbstractPoolManager {


    public ActiveMQPipelineRunner(RTScanner rtScanner) {
    }

}
