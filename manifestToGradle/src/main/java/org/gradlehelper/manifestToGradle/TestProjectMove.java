package org.gradlehelper.manifestToGradle;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.gradlehelper.manifestToGradle.tools.CliArguments;

/**
 * Moves test project files from own artifact into host project artifact. The following tasks are performed:
 * 1) Move some.namespace.test/src/test to some.namespace/src/test
 * 2) Move some.namespace.test/lib to some.namespace/libTests
 *
 * In a second step the integration tests are separated from the unit tests:
 * 1) some.namespace/src/test keeps the unit tests
 * 2) some.namespace/src/integration-test is created and populated with all classes that inherit from "OSGiTest"
 *
 * @author David Graeff
 */
public class TestProjectMove {
    private void moveFromOwnBundleToHostBundle(Path testProject) {
        Path hostProjectDir = testProject.resolveSibling(testProject.getFileName().toString().replace(".test", ""));
        Path testsDirectory = testProject.resolve("src/test");
        if (Files.exists(hostProjectDir.resolve("src")) && Files.exists(testsDirectory)) {
            Path hostProjectTestDir = hostProjectDir.resolve("src/test");
            System.out.println("\tMove project " + testProject.getFileName() + "/src/test/* -> "
                    + hostProjectDir.getFileName() + "/src/test");
            try (Stream<Path> testFiles = Files.find(testsDirectory, Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile()
                            && (filePath.toString().endsWith(".java") || filePath.toString().endsWith(".groovy")))) {
                testFiles.forEach(testFilepath -> moveTestFile(hostProjectDir, testsDirectory, testFilepath));
            } catch (IOException e) {
                System.err.println("\t\tFailed: From " + testsDirectory.toString() + " to "
                        + hostProjectTestDir.toString() + "; " + e.getMessage());
            }
        }

        Path libDirectory = testProject.resolve("lib");
        if (Files.exists(libDirectory)) {
            Path hostProjectTestLibDir = hostProjectDir.resolve("libTests");
            try {
                System.out.println("\tMove project libs of " + testProject.getFileName().toString());
                Files.move(libDirectory, hostProjectTestLibDir);
            } catch (IOException e) {
                System.err.println(
                        "\t\tFailed: From " + libDirectory.toString() + " to " + hostProjectTestLibDir.toString());
            }
        }

        Path resDirectory = testsDirectory.resolve("resources");
        if (Files.exists(resDirectory)) {
            Path hostProjectTestResDir = hostProjectDir.resolve("src/test/resources");
            try {
                System.out.println("\tMove test resources of " + testProject.getFileName().toString());
                Files.move(resDirectory, hostProjectTestResDir);
            } catch (IOException e) {
                System.err.println(
                        "\t\tFailed: From " + resDirectory.toString() + " to " + hostProjectTestResDir.toString());
            }
        }
    }

    /**
     * Move for example file
     * namespace.test/src/test/java|groovy/lala/bla/abc.java to
     * namespace/src/test/java|groovy/lala/bla/abc.java.
     *
     * If the file contains "OSGiTest" it is moved to src/integration-test/...
     *
     * @param destinationProjectDir
     * @param sourceTestDir
     * @param testFile
     */
    private void moveTestFile(Path destinationProjectDir, Path sourceTestDir, Path testFile) {

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(testFile.toFile())))) {
            Path destFile = destinationProjectDir;
            if (bufferedReader.lines().anyMatch(line -> line.contains("OSGiTest"))) {
                destFile = destFile.resolve("src/integration-test");
            } else {
                destFile = destFile.resolve("src/test");

            }
            destFile = destFile.resolve(sourceTestDir.relativize(testFile));

            Files.createDirectories(destFile.getParent());
            Files.move(testFile, destFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("\t\tFailed " + e.getMessage());
        }
    }

    public void start(CliArguments arguments) throws IOException {
        System.out.println("Start test project move");
        Files.find(arguments.inputDir, Integer.MAX_VALUE, (filePath, fileAttr) -> {
            if (!fileAttr.isDirectory()) {
                return false;
            }
            return (filePath.toString().endsWith(".test") && filePath
                    .resolveSibling(filePath.getFileName().toString().replace(".test", "")).toFile().exists());
        }).forEach(this::moveFromOwnBundleToHostBundle);
        System.out.println("Finish test project move");
    }
}
