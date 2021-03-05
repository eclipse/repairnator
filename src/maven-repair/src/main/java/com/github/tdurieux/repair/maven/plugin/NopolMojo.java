package com.github.tdurieux.repair.maven.plugin;

import com.google.common.io.ByteStreams;
import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;
import fr.inria.lille.repair.common.config.NopolContext;
import fr.inria.lille.repair.common.patch.Patch;
import fr.inria.lille.repair.common.synth.RepairType;
import fr.inria.lille.repair.nopol.NoPol;
import fr.inria.lille.repair.nopol.NopolResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.inria.spirals.repairnator.utils.Utils.checkToolsJar;

@Mojo( name = "nopol", aggregator = true,
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class NopolMojo extends AbstractRepairMojo {

    private static String HARDCODED_NOPOL_VERSION = "666abb764bf1819f6c316faf4fe5b559ac583de1";

    @Parameter( defaultValue = "${project.build.directory}/nopol", property = "outputDir", required = true )
    private File outputDirectory;

    @Parameter( defaultValue = "pre_then_cond", property = "type", required = true )
    private String type;

    @Parameter( defaultValue = "10", property = "maxTime", required = true )
    private int maxTime;

    @Parameter( defaultValue = "cocospoon", property = "localizer", required = true )
    private String localizer;

    @Parameter( defaultValue = "smt", property = "synthesis", required = true )
    private String synthesis;

    @Parameter( defaultValue = "z3", property = "solver", required = true )
    private String solver;

	private NopolResult result;

	@Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            checkToolsJar();
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("tools.jar has not been loaded, therefore Nopol can't run");
        }

        final List<String> failingTestCases = getFailingTests();
        final List<URL> dependencies = getClasspath();
        final List<File> sourceFolders = getSourceFolders();

        System.out.println(failingTestCases.size() + " detected failing test classes. (" + StringUtils.join(failingTestCases,":") + ")");

        final List<URL> nopolClasspath = getNopolClasspath();
        final String systemClasspath = System.getProperty("java.class.path");

		String strClasspath = getStringClasspathFromList(nopolClasspath, systemClasspath);

        try {
            setGzoltarDebug(true);
            System.setProperty("java.class.path", strClasspath);
            NopolContext nopolContext = createNopolContext(failingTestCases, dependencies, sourceFolders);

            try {
                File currentDir = new File(".").getCanonicalFile();
                nopolContext.setRootProject(currentDir.toPath().toAbsolutePath());
            } catch (IOException e) {
                getLog().error("Error while setting the root project path, the created patches might have absolute paths.");
            }
            
            final NoPol nopol = new NoPol(nopolContext);
            this.result = nopol.build();
            printResults(result);
        } finally {
            System.setProperty("java.class.path", systemClasspath);
        }
    }

    private void printResults(NopolResult result) {
        System.out.println("Nopol executed after: "+result.getDurationInMilliseconds()+" ms.");
        System.out.println("Status: "+result.getNopolStatus());
        System.out.println("Angelic values: "+result.getNbAngelicValues());
        System.out.println("Nb statements: "+result.getNbStatements());
        if (result.getPatches().size() > 0) {
            for (Patch p : result.getPatches()) {
                System.out.println("Obtained patch: "+p.asString());
            }
        }
    }

    private NopolContext createNopolContext(List<String> failingTestCases,
            List<URL> dependencies, List<File> sourceFolders) {
        NopolContext nopolContext = new NopolContext(sourceFolders.toArray(new File[0]), dependencies.toArray(new URL[0]), failingTestCases.toArray(new String[0]), Collections.<String>emptyList());
        nopolContext.setComplianceLevel(getComplianceLevel());
        nopolContext.setTimeoutTestExecution(300);
        nopolContext.setMaxTimeEachTypeOfFixInMinutes(15);
        nopolContext.setMaxTimeInMinutes(maxTime);
        nopolContext.setLocalizer(this.resolveLocalizer());
        nopolContext.setSynthesis(this.resolveSynthesis());
        nopolContext.setType(this.resolveType());
        nopolContext.setOnlyOneSynthesisResult(true);
        nopolContext.setJson(true);
        if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
        nopolContext.setOutputFolder(outputDirectory.getAbsolutePath());

        NopolContext.NopolSolver solver = this.resolveSolver();
        nopolContext.setSolver(solver);

        if (nopolContext.getSynthesis() == NopolContext.NopolSynthesis.SMT) {
            if (solver == NopolContext.NopolSolver.Z3) {
                String z3Path = this.loadZ3AndGivePath();
                SolverFactory.setSolver(solver, z3Path);
                nopolContext.setSolverPath(z3Path);
            } else {
                SolverFactory.setSolver(solver, null);
            }
        }
        return nopolContext;
    }

    private NopolContext.NopolSolver resolveSolver() {
        try {
            return NopolContext.NopolSolver.valueOf(solver.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Solver value \""+solver+"\" is wrong. Only following values are accepted: "+StringUtils.join(NopolContext.NopolSolver.values(),", "));
        }
    }

    private NopolContext.NopolLocalizer resolveLocalizer() {
        try {
            return NopolContext.NopolLocalizer.valueOf(localizer.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Localizer value \""+localizer+"\" is wrong. Only following values are accepted: "+StringUtils.join(NopolContext.NopolLocalizer.values(), ","));
        }
    }

    private NopolContext.NopolSynthesis resolveSynthesis() {
        try {
            return NopolContext.NopolSynthesis.valueOf(synthesis.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Synthesis value \""+synthesis+"\" is wrong. Only following values are accepted: "+StringUtils.join(NopolContext.NopolSynthesis.values(), ","));
        }
    }

    private RepairType resolveType() {
        try {
            return RepairType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Type value \""+type+"\" is wrong. Only following values are accepted: "+StringUtils.join(RepairType.values(), ","));
        }
    }

    private String loadZ3AndGivePath() {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");

        String resourcePath = (isMac)? "z3/z3_for_mac" : "z3/z3_for_linux";
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(resourcePath);

        try {
            Path tempFilePath = Files.createTempFile("nopol", "z3");
            byte[] content = ByteStreams.toByteArray(in);
            Files.write(tempFilePath, content);

            tempFilePath.toFile().setExecutable(true);
            return tempFilePath.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<URL> getNopolClasspath() {
        List<URL> classpath = new ArrayList<>();
        Artifact artifactPom = artifactFactory.createArtifact("fr.inria.gforge.spirals","nopol", HARDCODED_NOPOL_VERSION, null, "pom");
        Artifact artifactJar = artifactFactory.createArtifact("fr.inria.gforge.spirals","nopol", HARDCODED_NOPOL_VERSION, null, "jar");
        File filePom = new File(localRepository.getBasedir() + "/" + localRepository.pathOf(artifactPom));
        File fileJar = new File(localRepository.getBasedir() + "/" + localRepository.pathOf(artifactJar));

		classpath.addAll(getClassPathFromPom(filePom, fileJar));

        return classpath;
    }

    @Override
    public List<URL> getClasspath() {
        List<URL> classpath = super.getClasspath();

        Artifact artifactJar = artifactFactory.createArtifact("fr.inria.gforge.spirals","nopol", HARDCODED_NOPOL_VERSION, null, "jar");
        File fileJar = new File(localRepository.getBasedir() + "/" + localRepository.pathOf(artifactJar));

        try {
            if (fileJar.exists()) {
                classpath.add(fileJar.toURI().toURL());
            }
            String path = System.getProperty("java.home") + "/../lib/tools.jar";
            File jarFile = new File(path);
            if (jarFile.exists()) {
                classpath.add(jarFile.toURI().toURL());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error occurred, dependency will be passed: "+e.getMessage());
        }

        return new ArrayList<>(classpath);
    }

	public NopolResult getResult() {
		return result;
	}
}
