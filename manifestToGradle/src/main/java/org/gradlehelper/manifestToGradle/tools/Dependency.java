package org.gradlehelper.manifestToGradle.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represent a dependency. Contains maven coordinates and transitive dependencies.
 */
public class Dependency implements Comparable<Dependency> {
    public String group = "";
    public String name = "";
    public String version = "";
    protected String latestKnownVersion = "";
    Map<String, Dependency> transitiveDependecy = new HashMap<>();

    protected Dependency() {
    }

    public Dependency(String name, String group, String version) {
        this.name = name;
        this.group = group;
        this.version = version;
        this.latestKnownVersion = version;
    }

    public void addTransitiveDependency(Dependency d) {
        if (d != null) {
            transitiveDependecy.put(d.toString(), d);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Dependency)) {
            return false;
        }
        Dependency d = (Dependency) obj;
        return name.equals(d.name) && group.equals(d.group) && version.equals(d.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version);
    }

    @Override
    public int compareTo(Dependency o) {
        return (group + name).compareTo(o.group + o.name);
    }

    @Override
    public String toString() {
        return group + ":" + name + ":" + version;
    }

    /**
     * Create a dependency by group:name:version coordinates
     *
     * @param string A string containing the group, name and version
     * @return Returns a dependecy or null if the input is invalid
     */
    public static Dependency ByMavenCoord(String line, String... transitive) {
        String[] coordinates = line.split(":");
        Dependency dependency = new Dependency(coordinates[1], coordinates[0], coordinates[2]);
        for (String dep : transitive) {
            String[] tCoordinates = dep.split(":");
            dependency.addTransitiveDependency(new Dependency(tCoordinates[1], tCoordinates[0], tCoordinates[2]));
        }
        return dependency;
    }

    public boolean hasTransitiveDependencies() {
        return !transitiveDependecy.isEmpty();
    }

    public Collection<Dependency> getTransitiveDeps() {
        return transitiveDependecy.values();
    }
}