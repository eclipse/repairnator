package com.github.tdurieux.repair.maven.plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo( name = "cardumen", aggregator = true,
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class CardumenMojo extends GenProgMojo {

    public CardumenMojo() {
        super.mode = "cardumen";
    }
}
