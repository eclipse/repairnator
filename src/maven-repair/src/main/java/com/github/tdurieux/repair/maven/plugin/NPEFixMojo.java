package com.github.tdurieux.repair.maven.plugin;

import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "npefix", aggregator = true,
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class NPEFixMojo extends AbstractNPEFixMojo {

    public void execute() throws MojoExecutionException {
        this.repairStep = new NPERepair();
        super.execute();

    }

}
