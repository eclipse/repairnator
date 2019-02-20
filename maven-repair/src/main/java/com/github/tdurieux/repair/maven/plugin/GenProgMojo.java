package com.github.tdurieux.repair.maven.plugin;

import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.main.evolution.AstorMain;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Mojo( name = "jGenProg", aggregator = true,
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class GenProgMojo extends AbstractRepairMojo {

    private static String HARDCODED_ASTOR_VERSION = "0.0.2-SNAPSHOT";

    @Parameter( defaultValue = "${project.build.directory}/astor", property = "out", required = true )
    private File outputDirectory;

    @Parameter(property = "packageToInstrument")
    private String packageToInstrument;

    @Parameter(defaultValue = "local", property = "scope")
    private String scope;

    @Parameter( defaultValue = "0.1", property = "thfl")
    private double localisationThreshold;

    @Parameter( defaultValue = "10", property = "seed")
    private int seed;

    @Parameter( defaultValue = "200", property = "maxgen")
    private int maxgen;

    @Parameter( defaultValue = "100", property = "maxtime")
    private int maxtime;

    @Parameter( defaultValue = "true", property = "stopfirst")
    private boolean stopfirst;

    @Parameter( defaultValue = "false", property = "skipfaultlocalization")
    private boolean skipfaultlocalization;


    protected String mode = "statement";

    private List<ProgramVariant> output;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final List<URL> astorClasspath = getAstorClasspath();
        final String systemClasspath = System.getProperty("java.class.path");

        final String strClasspath = getStringClasspathFromList(astorClasspath, systemClasspath);

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        try {
            setGzoltarDebug(true);
            System.setProperty("java.class.path", strClasspath);
            AstorMain astor = new AstorMain();
            AstorContext context = createAstorContext();
            astor.execute(context.getAstorArgs());

            this.output = astor.getEngine().getSolutions();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.setProperty("java.class.path", systemClasspath);
        }
    }

    private AstorContext createAstorContext() {
	    AstorContext context = new AstorContext();
	    context.out = outputDirectory.getAbsolutePath();
	    context.Package = packageToInstrument;
	    context.scope = scope;
	    context.flThreshold = localisationThreshold;
        context.seed = seed;
        context.maxGen = maxgen;
        context.maxTime = maxtime;
        context.location = project.getBasedir().getAbsolutePath();
        context.stopFirst = stopfirst;
        context.mode = mode;
        context.javaComplianceLevel = getComplianceLevel();
        context.skipfaultlocalization = skipfaultlocalization;

        for (int i = 0; i < getFailingTests().size(); i++) {
            String test = getFailingTests().get(i);
            context.failing.add(test);
        }

        final List<URL> dependencies = getClasspath();

        for (MavenProject mavenProject : reactorProjects) {
            Build build = mavenProject.getBuild();
            context.srcJavaFolder.add(getRelativePath(build.getSourceDirectory()));
            context.srcTestFolder.add(getRelativePath(build.getTestSourceDirectory()));
            context.binJavaFolder.add(getRelativePath(build.getOutputDirectory()));
            context.binTestFolder.add(getRelativePath(build.getTestOutputDirectory()));
        }

        for (int i = 0; i < dependencies.size(); i++) {
            URL url = dependencies.get(i);
            String path = url.getPath();
            if (context.binTestFolder.contains(getRelativePath(path))
                    || context.binJavaFolder.contains(getRelativePath(path))) {
                continue;
            }
            context.dependencies.add(path);
        }
        if (context.dependencies.isEmpty()) {
            context.dependencies.add(dependencies.get(0).getPath());
        }


        return context;
    }

    private String getRelativePath(String path) {
	    if (path == null) {
	        return null;
        }
        if (!new File(path).exists()) {
	        return null;
        }
        path = path.replace(project.getBasedir().getAbsolutePath(), "");

	    if (!path.startsWith("/")) {
	        path = "/" + path;
        }
        if (path.endsWith("/")) {
	        path = path.substring(0, path.length() - 1);
        }
	    return path;
    }

    private List<URL> getAstorClasspath() {
        List<URL> classpath = new ArrayList<>();
        Artifact artifactPom = artifactFactory.createArtifact("org.inria.sacha.automaticRepair","astor", HARDCODED_ASTOR_VERSION, null, "pom");
        Artifact artifactJar = artifactFactory.createArtifact("org.inria.sacha.automaticRepair","astor", HARDCODED_ASTOR_VERSION, null, "jar");
        File filePom = new File(localRepository.getBasedir() + "/" + localRepository.pathOf(artifactPom));
        File fileJar = new File(localRepository.getBasedir() + "/" + localRepository.pathOf(artifactJar));

        classpath.addAll(getClassPathFromPom(filePom, fileJar));
        return classpath;
    }

	public List<ProgramVariant> getResult() {
		return output;
	}
}
