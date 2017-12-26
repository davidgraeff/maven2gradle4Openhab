package org.gradlehelper.manifestToGradle;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.gradlehelper.manifestToGradle.tools.CliArguments;

/**
 * Append readme text of resource "additionalReadme.md" to projects main readme file.
 *
 * @author David Graeff
 */
public class AppendReadme {
    public void start(CliArguments arguments) throws IOException {
        Path readmeFile = arguments.inputDir.resolve("README.md");

        if (!Files.exists(readmeFile)) {
            System.err.println("Append readme: README.md not found");
            return;
        }

        System.out.println("Append readme");

        String readmeTxt = new BufferedReader(
                new InputStreamReader(getClass().getResource("/repository/readme.md").openStream())).lines()
                        .collect(Collectors.joining("\n"));

        String readmeFileContent = null;

        try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(readmeFile.toFile()))) {
            readmeFileContent = new BufferedReader(inputStreamReader).lines().collect(Collectors.joining("\n"));
        }

        if (readmeFileContent == null) {
            System.err.println("Append readme: README.md could not be read");
            return;
        }

        // Cut out old appended sections
        int iStart = readmeFileContent.indexOf("## The build system");
        if (iStart != -1) {
            int iEnd = readmeFileContent.indexOf("<br>", iStart);
            if (iEnd != -1) {
                readmeFileContent = readmeFileContent.substring(0, iStart) + readmeFileContent.substring(iEnd + 4);
                readmeFileContent = readmeFileContent.trim();
            }
        }

        readmeFileContent += "\n" + readmeTxt;
        try (FileOutputStream fileOutputStream = new FileOutputStream(readmeFile.toFile(), false)) {
            fileOutputStream.getChannel().truncate(0);
            fileOutputStream.getChannel().force(true);
            try (OutputStreamWriter out = new OutputStreamWriter(fileOutputStream)) {
                out.write(readmeFileContent);
            }
        }
    }
}
