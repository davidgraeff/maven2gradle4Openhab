package org.gradlehelper.manifestToGradle;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.gradlehelper.manifestToGradle.tools.CliArguments;

/**
 * Deletes test project directories
 *
 * @author David Graeff
 */
public class TestProjectDelete {
    private void deleteTestDirectory(Path testProject) {
        try {
            System.out.println("\tRemove test project directory " + testProject.getFileName().toString());
            Files.walkFileTree(testProject, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("\t\tFailed " + e.getMessage());
        }
    }

    public void start(CliArguments arguments) throws IOException {
        System.out.println("Start test project removal");
        Files.find(arguments.inputDir, Integer.MAX_VALUE, (filePath, fileAttr) -> {
            if (!fileAttr.isDirectory()) {
                return false;
            }
            return (filePath.toString().endsWith(".test") && filePath
                    .resolveSibling(filePath.getFileName().toString().replace(".test", "")).toFile().exists());
        }).forEach(this::deleteTestDirectory);
        System.out.println("Finish test project removal");
    }
}
