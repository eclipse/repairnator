package com.github.tdurieux.repair.maven.plugin;

import fr.inria.spirals.repairnator.process.step.repair.NPERepairSafe;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "npefix-safe", aggregator = true,
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class NPEFixSafeMojo extends AbstractNPEFixMojo {

    public void execute() throws MojoExecutionException {
        this.repairStep = new NPERepairSafe("safe-mono", nbIteration, scope, repairStrategy);
        super.execute();
    }

}
