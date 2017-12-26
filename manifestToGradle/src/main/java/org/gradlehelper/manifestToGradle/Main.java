package org.gradlehelper.manifestToGradle;

import java.io.IOException;

import org.gradlehelper.manifestToGradle.tools.CliArguments;

import picocli.CommandLine;

public class Main {

    public static void main(String[] args) throws IOException {
        CliArguments arguments = new CliArguments();
        CommandLine cli = new CommandLine(arguments);
        cli.registerConverter(java.nio.file.Path.class, s -> java.nio.file.Paths.get(s));
        cli.parse(args);
        if (cli.isUsageHelpRequested()) {
            CommandLine.usage(arguments, System.err);
            return;
        }
        if (cli.isVersionHelpRequested()) {
            cli.printVersionHelp(System.err);
            return;
        }

        System.out.println("Processing " + arguments.inputDir);

        new RemoveEclipseFiles().start(arguments);
        new TestProjectMove().start(arguments);
        new ManifestToGradle().start(arguments);
        new CopyRootProjectFiles().start(arguments);
        new AppendReadme().start(arguments);
        new TestProjectDelete().start(arguments);
    }
}
