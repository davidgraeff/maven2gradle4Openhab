package org.gradlehelper.manifestToGradle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.gradlehelper.manifestToGradle.tools.CliArguments;

/**
 * Copy the multi project gradle files to the user given root directory
 *
 * @author David Graeff
 */
public class CopyRootProjectFiles {
    public void start(CliArguments arguments) throws IOException {
        System.out.println("Copy multi build gradle files");
        CopyOption o = null;
        if (arguments.overrideExisting) {
            o = StandardCopyOption.REPLACE_EXISTING;
        }
        String files[] = { "build.gradle", "settings.gradle", "gradle.properties", "archetype.gradle",
                "codequality.gradle", "deploy.gradle" };
        for (String file : files) {
            try (InputStream in = getClass().getResource("/repository/" + file).openStream()) {
                Files.copy(in, arguments.inputDir.resolve(file), o);
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
            }
        }
        try (InputStream in = getClass().getResource("/repository/license_template.txt").openStream()) {
            Files.createDirectories(arguments.inputDir.resolve("project-orga"));
            Files.copy(in, arguments.inputDir.resolve("project-orga/license_template.txt"), o);
        } catch (java.nio.file.FileAlreadyExistsException ignored) {
        }
    }
}
