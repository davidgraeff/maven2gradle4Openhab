/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.gradlehelper.manifestToGradle;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jdt.annotation.NonNull;
import org.gradlehelper.manifestToGradle.tools.CliArguments;
import org.gradlehelper.manifestToGradle.tools.DepWithGradleType;
import org.gradlehelper.manifestToGradle.tools.Dependency;
import org.gradlehelper.manifestToGradle.tools.DependencyResolver;

/*
 * Reads manifest files and creates gradle build files out of them
 */
public class ManifestToGradle {
    private Set<String> filterDependecies = Collections.emptySet();
    private int processedFiles = 0;
    private Map<Dependency, String> fixedDependencies;
    private DependencyResolver groupResolver;

    // We define an order of gradle dependency types ('compile', etc).
    // The reasoning is, if a dependency is declared as 'testCompile', so used
    // by a test only and later on also declared as 'compile', then we only
    // need to keep the 'compile' variant.
    private static Map<String, Integer> gradleDepTypeOrder = new HashMap<>();
    static {
        gradleDepTypeOrder.put("api", 0);
        gradleDepTypeOrder.put("compile", 1);
        gradleDepTypeOrder.put("implementation", 2);
        gradleDepTypeOrder.put("compileOnly", 3);
        gradleDepTypeOrder.put("testImplementation", 5);
        gradleDepTypeOrder.put("testCompile", 6);
        gradleDepTypeOrder.put("runtime", 10);
        gradleDepTypeOrder.put("runtimeOnly", 10);
        gradleDepTypeOrder.put("testRuntime", 11);
        gradleDepTypeOrder.put("testRuntimeOnly", 11);
    }

    private void writeManifestEntryIfExisting(PrintWriter gradleOutput, Attributes attributes, String attribKey) {
        String value = attributes.getValue(attribKey);
        if (value != null) {
            gradleOutput.append(",\n\t\t\"").append(attribKey).append("\" : \"").append(value).append("\"");
        }
    }

    private void writeBundleMetadata(PrintWriter gradleOutput, Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();
        gradleOutput
                .append("jar {\n\tmanifest {\n\t\tattributes(\"Bundle-RequiredExecutionEnvironment\" : \"JavaSE-1.8\"");
        writeManifestEntryIfExisting(gradleOutput, attributes, "Bundle-ClassPath");
        writeManifestEntryIfExisting(gradleOutput, attributes, "Bundle-Activator");
        writeManifestEntryIfExisting(gradleOutput, attributes, "Bundle-Vendor");
        gradleOutput.append(")\n\t}\n}\n");
    }

    @SuppressWarnings("null")
    private void writeManifest(Path manifestFile) {
        Path projectDir = manifestFile.getParent().getParent();
        Path projectTestDir = projectDir.getParent().resolve(projectDir.getFileName().toString() + ".test");
        Path testManifestFile = projectTestDir.resolve("META-INF/MANIFEST.MF");

        Path pomFile = projectDir.resolve("pom.xml");
        @NonNull
        Model model;
        try (FileReader reader = new FileReader(pomFile.toFile())) {
            MavenXpp3Reader mavenreader = new MavenXpp3Reader();
            model = mavenreader.read(reader);
            model.setPomFile(pomFile.toFile());
        } catch (Exception ex) {
            System.err.println("Failed to open pom file: " + projectDir.getFileName().toString());
            return;
        }

        // Read dependencies from main manifest file and also from .test project manifest file
        Map<Dependency, String> dependencies = new HashMap<>();
        // Add additional dependencies, declared in resource file
        dependencies.putAll(fixedDependencies);

        // Add groovy as dependency if it's a groovy project
        if (Files.exists(projectDir.resolve("src/test/groovy"))) {
            dependencies.put(new Dependency("groovy-all", "org.codehaus.groovy", "+"), "testCompile");
        }
        if (Files.exists(projectDir.resolve("src/main/groovy"))) {
            dependencies.put(new Dependency("groovy-all", "org.codehaus.groovy", "+"), "compile");
        }
        // Add dependencies of pom.xml as compileOnly
        for (org.apache.maven.model.Dependency dependency : model.getDependencies()) {
            dependencies.put(new Dependency(dependency.getArtifactId(), dependency.getGroupId(), "+"), "compileOnly");
        }

        // Read MANIFEST.MF of current "project" and of "project.test" if existing. Add found dependencies.
        Manifest manifest;
        try {
            manifest = new Manifest(new BufferedInputStream(new FileInputStream(manifestFile.toString())));
            Set<String> filter = new HashSet<>();
            filter.addAll(readManifestExports(manifest));
            filter.addAll(filterDependecies);
            readManifestDependencies(manifest, dependencies, filter, false);
            if (Files.exists(testManifestFile)) {
                System.out.println("Test dependencies detected: " + projectDir.getFileName());
                Manifest tManifest = new Manifest(
                        new BufferedInputStream(new FileInputStream(testManifestFile.toString())));
                filter.addAll(readManifestExports(tManifest));
                readManifestDependencies(tManifest, dependencies, filter, true);
            }
        } catch (IOException e) {
            System.err.println("Failed to process " + manifestFile + ": " + e.getMessage());
            return;
        }

        ++processedFiles;

        // Write details.gradle
        Path outputPath = projectDir.resolve("settings.gradle");
        try (PrintWriter gradleOutput = new PrintWriter(outputPath.toString())) {
            gradleOutput.append("rootProject.name='").append(model.getArtifactId()).println("'");
        } catch (IOException e) {
            System.err.println("Failed to write gradle details file: " + outputPath.toString());
        }

        outputPath = projectDir.resolve("manifest.gradle");
        try (PrintWriter gradleOutput = new PrintWriter(outputPath.toString())) {
            if (model.getGroupId() != null) {
                gradleOutput.append("group='").append(model.getGroupId()).println("'");
            } else if (model.getParent().getGroupId() != null) {
                gradleOutput.append("group='").append(model.getParent().getGroupId()).println("'");
            }
            if (model.getDescription() != null) {
                gradleOutput.append("description='").append(model.getDescription()).println("'");
            } else if (model.getName() != null) {
                gradleOutput.append("description='").append(model.getName()).println("'");
            }
            writeBundleMetadata(gradleOutput, manifest);
        } catch (IOException e) {
            System.err.println("Failed to write " + outputPath.toString() + ": " + e.getMessage());
        }

        outputPath = projectDir.resolve("dependencies.gradle");
        try (PrintWriter gradleOutput = new PrintWriter(outputPath.toString())) {
            writeDependencies(gradleOutput, dependencies, projectDir);
        } catch (IOException e) {
            System.err.println("Failed to write " + outputPath.toString() + ": " + e.getMessage());
        }

        try (InputStream in = getClass().getResource("/project/build.gradle").openStream()) {
            Files.copy(in, projectDir.resolve("build.gradle"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to write build.gradle: " + e.getMessage());
        }

        // Special case: groovy code
        if (Files.exists(projectDir.resolve("src/main/groovy"))
                || Files.exists(projectDir.resolve("src/test/groovy"))) {
            System.out.println("\tGroovy project detected: " + projectDir.getFileName());
            try (InputStream in = getClass().getResource("/project/groovySupport.gradle").openStream()) {
                Files.copy(in, projectDir.resolve("groovySupport.gradle"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Failed to write groovySupport.gradle: " + e.getMessage());
            }
        }

    }

    private static Set<String> readManifestExports(Manifest manifest) {
        String exportPackagesString = manifest.getMainAttributes().getValue("Export-Package");
        if (exportPackagesString == null) {
            return Collections.emptySet();
        } else {
            return Stream.of(exportPackagesString.split(",")).collect(Collectors.toSet());
        }
    }

    private void readManifestDependencies(Manifest manifest, Map<Dependency, String> dependencies, Set<String> filter,
            boolean isTest) {
        String importPackagesString = manifest.getMainAttributes().getValue("Import-Package");

        // Imported packages from manifest
        if (importPackagesString != null) {
            importPackagesString = importPackagesString.replaceAll("version=\"(.*?)\"", "");
            String[] arr = importPackagesString.split(",");
            for (String artifactName : arr) {
                artifactName = artifactName.split(";")[0];
                if (filter.contains(artifactName)) {
                    continue;
                }
                Dependency dependency = groupResolver.resolveDependency(artifactName);
                if (dependency == null) {
                    // Dependency resolution decided, that this dependency is not required
                    continue;
                }
                // We never define a version in the dependencies.gradle. The version will be restricted
                // by the allowed_dependencies.txt and the multi-project build script.
                dependency.version = "+";
                // The dependency might already be in the set, fetch the existing entry if any. Replace
                // it if the existing one is lower within the order, defined by gradleDepTypeOrder.
                String type = artifactName.contains("smarthome") || artifactName.contains("openhab") ? "api"
                        : "implementation";
                if (gradleDepTypeOrder.get(type) < gradleDepTypeOrder.getOrDefault(dependencies.get(dependency), 20)) {
                    // Add only if not in the filter list.
                    if (!filter.contains(dependency.name)) {
                        dependencies.put(dependency, type);
                    }
                }
            }
        }
    }

    private void writeDependencies(PrintWriter gradleOutput, Map<Dependency, String> dependencies, Path projectDir)
            throws IOException {

        gradleOutput.append("dependencies {\n");

        // File dependencies
        Path externalLibDir = projectDir.resolve("lib");
        if (Files.isDirectory(externalLibDir)) {
            Files.walk(externalLibDir).filter(Files::isRegularFile).forEach(file -> {
                gradleOutput.append("\tcompile name: '")
                        .append(file.getFileName().toString().toLowerCase().replace(".jar", "")).append("'\n");
            });
        }
        externalLibDir = projectDir.resolve("libTests");
        if (Files.isDirectory(externalLibDir)) {
            Files.walk(externalLibDir).filter(Files::isRegularFile).forEach(file -> {
                gradleOutput.append("\ttestCompile name: '")
                        .append(file.getFileName().toString().toLowerCase().replace(".jar", "")).append("'\n");
            });
        }

        dependencies.entrySet().stream()
                .sorted((a, b) -> a.getValue().compareToIgnoreCase(b.getValue()) * 100
                        + a.getKey().group.compareToIgnoreCase(b.getKey().group) * 10
                        + a.getKey().name.compareToIgnoreCase(b.getKey().name) * 1)
                .forEach(depEntry -> writeDependency(gradleOutput, depEntry.getKey(), depEntry.getValue(), 1));

        gradleOutput.append("}\n");

    }

    private void writeDependency(PrintWriter gradleOutput, Dependency dep, String type, int level) {
        if (dep.hasTransitiveDependencies()) {
            for (int i = 0; i < level; ++i) {
                gradleOutput.append('\t');
            }
            gradleOutput.append(type).append(" module(\"").append(dep.toString()).append("\") {\n");
            for (Dependency trans : dep.getTransitiveDeps()) {
                writeDependency(gradleOutput, trans, "implementation", level + 1);
            }
            for (int i = 0; i < level; ++i) {
                gradleOutput.append('\t');
            }
            gradleOutput.append("}\n");
        } else {
            for (int i = 0; i < level; ++i) {
                gradleOutput.append('\t');
            }
            gradleOutput.append(type).append(" '").append(dep.toString()).append("'\n");
        }
    }

    public int start(CliArguments arguments) throws IOException {
        groupResolver = new DependencyResolver(arguments.inputDir.resolve(arguments.cacheFile), arguments.maxAge,
                arguments.mavenCoordinateGuess, arguments.useMavenCentral);
        processedFiles = 0;

        System.out.println("Start manifest converter");

        fixedDependencies = new BufferedReader(
                new InputStreamReader(getClass().getResource("/fixedDependencies.txt").openStream())).lines()
                        .map(line -> new DepWithGradleType(line)).collect(Collectors.toMap(d -> d, d -> d.gradletype));

        filterDependecies = new BufferedReader(
                new InputStreamReader(getClass().getResource("/filterDependencies.txt").openStream())).lines()
                        .collect(Collectors.toSet());

        Files.find(arguments.inputDir, Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile()
                        && "MANIFEST.MF".equals(filePath.getFileName().toString())
                        && "META-INF".equals(filePath.getParent().getFileName().toString()))
                .forEach(this::writeManifest);

        System.out.printf("Finished. Processed %d\n", processedFiles);
        groupResolver.writeCache();
        return processedFiles;
    }

    public int getProcessedFiles() {
        return processedFiles;
    }
}
