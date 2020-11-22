package com.github.tdurieux.repair.maven.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class AbstractRepairMojo extends AbstractMojo {

    @Parameter(property = "java.version", defaultValue = "-1")
    private String javaVersion;

    @Parameter(property = "maven.compiler.source", defaultValue = "-1")
    private String source;

    @Parameter(property = "maven.compile.source", defaultValue = "-1")
    private String oldSource;

    @Component
    protected ArtifactFactory artifactFactory;

    @Parameter(defaultValue="${localRepository}")
    protected ArtifactRepository localRepository;

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    protected MavenProject project;

    @Parameter( defaultValue = "${reactorProjects}", readonly = true )
	protected List<MavenProject> reactorProjects;

    public int getComplianceLevel() {
        int complianceLevel = 7;
        if (!source.equals("-1")) {
            complianceLevel = Integer.parseInt(source.substring(2));
        } else if (!oldSource.equals("-1")) {
            complianceLevel = Integer.parseInt(oldSource.substring(2));
        } else if (!javaVersion.equals("-1")) {
            complianceLevel = Integer.parseInt(javaVersion.substring(2, 3));
        }
        return complianceLevel;
    }

        private File getSurefireReportsDirectory( MavenProject subProject ) {
        String buildDir = subProject.getBuild().getDirectory();
        return new File( buildDir + "/surefire-reports" );
    }

    public List<String> getFailingTests() {
        List<String> result = new ArrayList<>();

        for (MavenProject mavenProject : reactorProjects) {
            File surefireReportsDirectory = getSurefireReportsDirectory(mavenProject);
            SurefireReportParser parser = new SurefireReportParser(Collections.singletonList(surefireReportsDirectory), Locale.ENGLISH, new NullConsoleLogger());

            try {
                List<ReportTestSuite> testSuites = parser.parseXMLReportFiles();
                for (ReportTestSuite reportTestSuite : testSuites) {
                    if (reportTestSuite.getNumberOfErrors()+reportTestSuite.getNumberOfFailures() > 0) {
                        result.add(reportTestSuite.getFullClassName());
                    }
                }
            } catch (MavenReportException e) {
                e.printStackTrace();
            }

        }

        return result;
    }

    public List<URL> getClasspath() {
        List<URL> classpath = new ArrayList<>();
        for (MavenProject mavenProject : reactorProjects) {
            try {
                for (String s : mavenProject.getTestClasspathElements()) {
                    File f = new File(s);
                    if (f.exists()) {
                        classpath.add(f.toURI().toURL());
                    }
                }
            } catch (DependencyResolutionRequiredException e) {
                continue;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>(classpath);
    }

	public List<File> getTestFolders() {
		Set<File> sourceFolder = new HashSet<>();
		for (MavenProject mavenProject : reactorProjects) {
			File sourceDirectory = new File(mavenProject.getBuild().getTestSourceDirectory());
			if (sourceDirectory.exists()) {
				sourceFolder.add(sourceDirectory);
			}
		}
		return new ArrayList<>(sourceFolder);
	}

    public List<File> getSourceFolders() {
        Set<File> sourceFolder = new HashSet<>();
        for (MavenProject mavenProject : reactorProjects) {
            File sourceDirectory = new File(mavenProject.getBuild().getSourceDirectory());
            if (sourceDirectory.exists()) {
                sourceFolder.add(sourceDirectory);
            }

            File generatedSourceDirectory = new File(mavenProject.getBuild().getOutputDirectory() + "/generated-sources");
            if (generatedSourceDirectory.exists()) {
                sourceFolder.add(generatedSourceDirectory);
            }
        }
        return new ArrayList<>(sourceFolder);
    }

    protected void setGzoltarDebug(boolean debugValue) {
        try {
            Field debug = com.gzoltar.core.agent.Launcher.class.getDeclaredField("debug");
            debug.setAccessible(true);
            debug.setBoolean(null, debugValue);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    protected String getStringClasspathFromList(List<URL> classpathList, String systemClasspath) {
        final StringBuilder sb = new StringBuilder(systemClasspath);
        if (sb.lastIndexOf(":") != sb.length() - 1) {
            sb.append(":");
        }
        for (int i = 0; i < classpathList.size(); i++) {
            URL url = classpathList.get(i);
            if (systemClasspath.contains(url.getPath())) {
                continue;
            }
            sb.append(url.getPath());
            if (i < classpathList.size() - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }


    protected List<URL> getClassPathFromPom(File filePom, File fileJar) {
        List<URL> classpath = new ArrayList<>();
        if (filePom.exists()) {
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            try (FileReader reader = new FileReader(filePom)) {
                Model model = pomReader.read(reader);

                List<Dependency> dependencies = model.getDependencies();
                for (Dependency dependency : dependencies) {
                    if (!dependency.isOptional() && dependency.getScope() == null && dependency.getVersion() != null) {
                        Artifact artifact = artifactFactory.createArtifact(dependency.getGroupId(),dependency.getArtifactId(), dependency.getVersion(), null, dependency.getType());
                        File jarFile = new File(localRepository.getBasedir() + "/" + localRepository.pathOf(artifact));

                        classpath.add(jarFile.toURI().toURL());
                    } else if ("system".equals(dependency.getScope())) {
                        String path = dependency.getSystemPath().replace("${java.home}", System.getProperty("java.home"));
                        File jarFile = new File(path);
                        if (jarFile.exists()) {
                            classpath.add(jarFile.toURI().toURL());
                        }
                    }
                }
                classpath.add(fileJar.toURI().toURL());
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error occured, dependency will be passed: "+e.getMessage());
            }
        }
        return classpath;
    }
}
