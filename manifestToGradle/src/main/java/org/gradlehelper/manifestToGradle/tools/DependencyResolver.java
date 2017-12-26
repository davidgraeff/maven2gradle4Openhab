package org.gradlehelper.manifestToGradle.tools;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Resolves dependencies, provided by package class names, with a best effort approach. Maven central and a static
 * list is used.
 */
public class DependencyResolver {
    // This map is populated in the constructor from a cache file
    private final Map<String, Dependency> dependencyCache;
    private final Path cacheFile;
    private int resolved = 0;
    private final long oldestArchive;
    private boolean mavenCoordinateGuess;
    private boolean useMavenCentral;

    private static Map<String, Dependency> smarthomePackages = new HashMap<>();

    private static void addSmarthomePackage(Dependency... dependencies) {
        for (Dependency dep : dependencies) {
            smarthomePackages.put(dep.name, dep);
        }
    }

    {
        addSmarthomePackage(Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.audio:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.autoupdate:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.binding.xml:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.extension.sample:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.id.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.id:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.persistence:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.scheduler:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.scriptengine:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.thing.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.thing.xml:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.thing:+",
                        "org.osgi:org.osgi.util.tracker:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.transform.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.transform:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core.voice:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.core:org.eclipse.smarthome.core:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.config:org.eclipse.smarthome.config.core:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.config:org.eclipse.smarthome.config.discovery.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.config:org.eclipse.smarthome.config.discovery:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.config:org.eclipse.smarthome.config.dispatch:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.config:org.eclipse.smarthome.config.xml:+"),
                Dependency
                        .ByMavenCoord("org.eclipse.smarthome.automation:org.eclipse.smarthome.automation.core.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.console.eclipse:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.console.karaf:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.console.rfc147:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.console:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.javasound:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.monitor:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.net.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.net:+",
                        "org.apache.commons:commons-exec:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest.core.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest.core:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest.mdns:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest.sitemap:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest.sse.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest.sse:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.rest:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.transport.mdns:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.transport.mqtt:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.transport.upnp.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.io:org.eclipse.smarthome.io.transport.upnp:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.core:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.item.runtime:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.item.tests:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.item.ui:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.item:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.lazygen:+"),
                Dependency
                        .ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.persistence.runtime:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.persistence.tests:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.persistence.ui:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.persistence:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.rule.runtime:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.rule.tests:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.rule.ui:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.rule:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.script.runtime:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.script.tests:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.script.ui:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.script:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.sitemap.runtime:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.sitemap:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.thing.runtime:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.thing.tests:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.thing.ui:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.model:org.eclipse.smarthome.model.thing:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.exec:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.javascript:+"),
                Dependency.ByMavenCoord(
                        "org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.jsonpath.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.jsonpath:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.map.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.map:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.regex.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.regex:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.scale.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.scale:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.xpath.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.xpath:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.xslt.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.transform:org.eclipse.smarthome.transform.xslt:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.storage:org.eclipse.smarthome.storage.mapdb.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.storage:org.eclipse.smarthome.storage.mapdb:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.voice:org.eclipse.smarthome.voice.mactts.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.voice:org.eclipse.smarthome.voice.mactts:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.ui:org.eclipse.smarthome.ui.classic:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.ui:org.eclipse.smarthome.ui.icon.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.ui:org.eclipse.smarthome.ui.icon:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.ui:org.eclipse.smarthome.ui.test:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.ui:org.eclipse.smarthome.ui.webapp:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.ui:org.eclipse.smarthome.ui:+"),
                Dependency.ByMavenCoord("org.eclipse.smarthome.test:org.eclipse.smarthome.test:+"));
    };

    private static class MavenCentralLookupDoc {
        String id;
        String g;
        String a;
        private String v;
        long timestamp;
        String latestVersion;
        String[] tags;

        String getVersion() {
            if (v != null) {
                return v;
            }
            if (latestVersion != null) {
                return latestVersion;
            }
            return "";
        }
    }

    private static class MavenCentralResponse {
        int numFound;
        int start;
        MavenCentralLookupDoc[] docs;
    }

    private static class MavenCentralLookup {
        MavenCentralResponse response;
    }

    /**
     * Create a new dependency resolver. A cache file is required.
     *
     * @param cacheFile A path to an existing or not existing cache file. The file will be loaded immediately and saved
     *            whenever writeCache() is called.
     * @param maxAge A maximum age for maven central packets as a unix timestamp
     * @param mavenCoordinateGuess Guess maven coordinates by manifest entry
     * @param useMavenCentral Use maven central API to find maven coordinates for manifest entry
     * @throws JsonIOException
     * @throws JsonSyntaxException
     * @throws FileNotFoundException
     */
    public DependencyResolver(Path cacheFile, long maxAge, boolean mavenCoordinateGuess, boolean useMavenCentral)
            throws JsonIOException, JsonSyntaxException, FileNotFoundException {
        this.cacheFile = cacheFile;
        this.oldestArchive = maxAge;
        this.mavenCoordinateGuess = mavenCoordinateGuess;
        this.useMavenCentral = useMavenCentral;
        Type typeOfHashMap = new TypeToken<Map<String, Dependency>>() {
        }.getType();
        if (Files.exists(cacheFile)) {
            Gson gson = new Gson();
            Map<String, Dependency> t = gson.fromJson(new FileReader(cacheFile.toFile()), typeOfHashMap);
            dependencyCache = t != null ? t : new HashMap<>();
        } else {
            dependencyCache = new HashMap<>();
        }
    }

    public @Nullable Dependency resolveDependency(String artifactName) {
        // Lookup cache
        Dependency dependency = dependencyCache.get(artifactName);

        // Lookup static patterns
        if (dependency == null) {
            if (artifactName.contains("org.eclipse.smarthome")) {
                Dependency dep = null;
                String artifactNameFragment = artifactName;
                while (artifactNameFragment != null && dep == null) {
                    dep = smarthomePackages.get(artifactNameFragment);
                    int dotIndex = artifactNameFragment.lastIndexOf(".");
                    if (dotIndex == -1) {
                        break;
                    }
                    artifactNameFragment = artifactNameFragment.substring(0, dotIndex);
                }
                if (dep == null) {
                    throw new RuntimeException("A smarthome package is not defined for: " + artifactName);
                }
                return dep;
            } else if (artifactName.contains("org.openhab.binding")) {
                return new Dependency(artifactName, "org.openhab.binding", "+");
            } else if (artifactName.contains("org.openhab")) {
                return new Dependency(artifactName, "org.openhab", "+");
            } else if (artifactName.contains("org.apache.commons.exec")) {
                return new Dependency("commons-exec", "org.apache.commons", "+");
            } else if (artifactName.contains("org.apache.commons")) {
                String dep = artifactName.split("\\.")[3];
                return new Dependency("commons-" + dep, "commons-" + dep, "+");
            } else if (artifactName.contains("com.google.common")) {
                return new Dependency("guava", "com.google.guava", "+");
            } else if (artifactName.contains("javax.servlet")) {
                return new Dependency("javax.servlet-api", "javax.servlet", "+");
            } else if (artifactName.contains("javax") && !artifactName.contains("jmdns")) {
                // javax SPI's shouldn't be part of the individual projects dependencies.
                // Except javax.jmdns which is only implemented by org.jmdns
                return null;
            }
        }

        // Lookup maven central
        if (dependency == null && useMavenCentral) {
            try {
                dependency = lookupMavenCentralViaArtifact(artifactName);
                if (dependency == null) {
                    dependency = lookupMavenCentralViaClassName(artifactName);
                }
                if (dependency != null) {
                    if (resolved % 10 == 0) {
                        writeCache();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (dependency == null) {
            dependency = new Dependency(artifactName, guessGroupByName(artifactName), "+");
        }

        if (!"NOT_RESOLVED".equals(dependency.group)) {
            dependencyCache.put(artifactName, dependency);
        }

        return dependency;
    }

    private String guessGroupByName(String name) {
        if (!mavenCoordinateGuess) {
            System.err.println("Maven coordinate not resolved for " + name);
            return "NOT_RESOLVED";
        }
        String[] parts = name.split("\\.");
        if (parts.length < 3) {
            return name;
        } else {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
    }

    private Dependency lookupMavenCentralViaClassName(String artifactName) throws IOException {
        System.out.println("\tMaven central lookup via class name " + artifactName + ": ");
        return lookupMavenCentral(artifactName,
                new URL("http://search.maven.org/solrsearch/select?rows=70&q=fc:%22" + artifactName + "%22"));
    }

    private Dependency lookupMavenCentralViaArtifact(String artifactName) throws IOException {
        System.out.println("\tMaven central lookup via artifact ID " + artifactName + ": ");
        return lookupMavenCentral(artifactName,
                new URL("http://search.maven.org/solrsearch/select?rows=10&q=a:%22" + artifactName + "%22"));
    }

    private Dependency lookupMavenCentral(String artifactName, URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        int code = urlConnection.getResponseCode();
        if (code != 200) {
            System.out.println("\t--not resolved (404)--");
            return null;
        }
        ++resolved;
        try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
            Gson gson = new Gson();
            MavenCentralLookup result = gson.fromJson(new InputStreamReader(in), MavenCentralLookup.class);
            if (result == null || result.response == null || result.response.numFound == 0) {
                System.out.println("\t--not resolved (no results)--");
                return null;
            }
            if (result.response.numFound == 1) {
                MavenCentralLookupDoc doc = result.response.docs[0];
                Dependency d = new Dependency(doc.a, doc.g, doc.v);
                System.out.println("\t" + d.toString());
                return d;
            } else {
                // Use a stream over potential dependencies
                Stream<MavenCentralLookupDoc> stream = Stream.of(result.response.docs)
                        // Filter "android" and Google webtoolkit specific packages
                        .filter(doc -> !doc.a.contains("-android") && !doc.a.contains("-gwt"))
                        .filter(DependencyResolver::filterByTags) // Filter "android" packages
                        // .filter(doc -> doc.timestamp > oldestArchive) // Filter too old packages
                        .sorted(DependencyResolver::compareVersion) // sort by version
                        // sort by longest matching substring between artifactID<-->group
                        .sorted((d1, d2) -> longestSubstr(d2.g, artifactName)
                                .compareTo(longestSubstr(d1.g, artifactName)));

                // System.out.println("Maven central lookup " + artifactName);
                // stream.forEach(d -> System.out.append("\t").println(d));
                Dependency d = stream.limit(1).map(doc -> new Dependency(doc.a, doc.g, doc.v)).findFirst().orElse(null);
                System.out.println("\t" + (d == null ? "--not resolved-- (results filtered)" : d.toString()));
                return d;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public void writeCache() throws IOException {
        try (FileWriter fileWriter = new FileWriter(cacheFile.toFile())) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(dependencyCache, fileWriter);
        }
    }

    /**
     * Don't accept maven central packages with "spam","temp","fixes" tags
     *
     * @param doc A maven central response document
     * @return Return true if the document is not tagged with any of the mentioned
     */
    private static boolean filterByTags(MavenCentralLookupDoc doc) {
        if (doc.tags == null) {
            return true;
        }
        return Stream.of(doc.tags)
                .noneMatch(s -> "spam".equals(s) || "temp".equals(s) || "fixes".equals(s) || "temporary".equals(s));
    }

    public static int compareVersion(MavenCentralLookupDoc subject, MavenCentralLookupDoc argument) {
        return new ComparableVersion(subject.getVersion()).compareTo(new ComparableVersion(argument.getVersion()));
    }

    public static Integer longestSubstr(String first, String second) {
        if (first == null || second == null || first.length() == 0 || second.length() == 0) {
            return 0;
        }

        int maxLen = 0;
        int fl = first.length();
        int sl = second.length();
        int[][] table = new int[fl][sl];

        for (int i = 0; i < fl; i++) {
            for (int j = 0; j < sl; j++) {
                if (first.charAt(i) == second.charAt(j)) {
                    if (i == 0 || j == 0) {
                        table[i][j] = 1;
                    } else {
                        table[i][j] = table[i - 1][j - 1] + 1;
                    }
                    if (table[i][j] > maxLen) {
                        maxLen = table[i][j];
                    }
                }
            }
        }
        return maxLen;
    }
}
