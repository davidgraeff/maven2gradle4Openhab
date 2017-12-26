package org.gradlehelper.manifestToGradle.tools;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ManifestToGradle", description = "Converts dependencies found in MANIFEST.MF files into gradle dependency blocks", footer = "Copyright(c) 2017", version = "1.0")
public class CliArguments {
    @Option(names = { "-i",
            "--input" }, paramLabel = "./", description = "Input directory. This and all subdirectories will be searched for META-INF/MANIFEST.MF files.")
    public Path inputDir = Paths.get("");

    @Option(names = { "-o",
            "--out" }, paramLabel = "allowed_dependencies.json", description = "Output filename for the allowed_dependencies.json file (default: %WORK_DIR%/allowed_dependencies.json)")
    public File outputFile;

    @Option(names = {
            "--max-age" }, description = "Filter packets from maven central that are older than the given unix timestamp. Default is 5 years. Use 0 to disable this filter.")
    public long maxAge = new Date().getTime() - 1000 * 60 * 60 * 24 * 365 * 5;

    @Option(names = {
            "--cache-file" }, description = "Dependency cache file. All resolved dependencies will be stored in this file")
    public String cacheFile = "dependency.cache.temp";

    @Option(names = {
            "--enable-maven-coordinate-guess" }, description = "A MANIFEST.MF bundle entry needs to be converted to maven artifactID and groupID. If maven central cannot help, the coordinates will be assumed to be: ArtifactID=manifest-entry, GroupID=first-three-package-names-of-manifest-entry")
    public boolean mavenCoordinateGuess = false;

    @Option(names = { "--override-existing" }, description = "Override existing build files. Default is true.")
    public boolean overrideExisting = true;

    @Option(names = { "--use-maven-central" }, description = "Disable dependency resolving via maven central")
    public boolean useMavenCentral = true;

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean usageHelpRequested = false;

    @Option(names = { "-v", "--version" }, versionHelp = true, description = "print version information and exit")
    private boolean versionRequested;

}
