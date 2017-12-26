package org.gradlehelper.manifestToGradle.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a dependency including the gradle type of the dependency, e.g. "compile"/"testCompile"/"runtime".
 */
public class DepWithGradleType extends Dependency {
    public transient String gradletype = null;

    /**
     * Constructor to create a dependency object from a gradle line like: "implementation 'org.slf4j:slf4j-api:+'"
     *
     * @param gradleLine A valid gradle line. Will throw an IllegalArgumentException if not valid.
     */
    public DepWithGradleType(String gradleLine) {
        Pattern pattern = Pattern.compile("(.*) '(.*):(.*):(.*)'");
        Matcher matcher = pattern.matcher(gradleLine);
        if (matcher.find()) {
            gradletype = matcher.group(1);
            group = matcher.group(2);
            name = matcher.group(3);
            version = matcher.group(4);
        } else {
            throw new IllegalArgumentException("Gradle line not valid: " + gradleLine);
        }
    }
}