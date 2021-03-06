package org.gradlehelper.manifestToGradle;

import java.io.IOException;
import java.nio.file.Files;

import org.gradlehelper.manifestToGradle.tools.CliArguments;

/**
 * Removes all eclipse project files, those are generated by gradle
 *
 * @author David Graeff
 */
public class RemoveEclipseFiles {
    public void start(CliArguments arguments) throws IOException {
        System.out.println("Start remove eclipse project files");
        Files.find(arguments.inputDir, Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && ".project".equals(filePath.getFileName().toString())
                        || "build.properties".equals(filePath.getFileName().toString()))
                .forEach(f -> f.toFile().delete());
        System.out.println("Finish remove eclipse project files");
    }
}
