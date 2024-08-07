/*
 * Copyright (C) 2024 The Verifyica project authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.antublue.verifyica.engine.support;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.junit.platform.commons.support.ReflectionSupport;

/** Class to implement ClassPathSupport */
@SuppressWarnings("deprecated")
public class ClassPathSupport {

    private static final ReentrantLock LOCK = new ReentrantLock(true);
    private static List<URI> URIS;

    /** Constructor */
    private ClassPathSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get a List of clas path URIs
     *
     * @return a List of class path URIs
     */
    public static List<URI> getClasspathURIs() {
        try {
            LOCK.lock();

            if (URIS == null) {
                Set<URI> uriSet = new LinkedHashSet<>();
                String classpath = System.getProperty("java.class.path");
                String[] paths = classpath.split(File.pathSeparator);
                for (String path : paths) {
                    uriSet.add(new File(path).toURI());
                }
                URIS = new ArrayList<>(uriSet);
            }
            return URIS;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Method to scan the Java class path and return a list of classes matching the Predicate
     *
     * @param predicate predicate
     * @return a List of Classes
     */
    public static List<Class<?>> findClasses(Predicate<Class<?>> predicate) {
        ArgumentSupport.notNull(predicate, "predicate is null");

        Set<Class<?>> set = new LinkedHashSet<>();
        getClasspathURIs()
                .forEach(
                        uri ->
                                set.addAll(
                                        ReflectionSupport.findAllClassesInClasspathRoot(
                                                uri, predicate, name -> true)));

        return new ArrayList<>(set);
    }

    /**
     * Method to scan the Java class path URI and return a list of classes matching the Predicate
     *
     * @param uri uri
     * @param predicate predicate
     * @return a List of Classes
     */
    public static List<Class<?>> findClasses(URI uri, Predicate<Class<?>> predicate) {
        ArgumentSupport.notNull(uri, "uri is null");
        ArgumentSupport.notNull(predicate, "predicate is null");

        return new ArrayList<>(
                ReflectionSupport.findAllClassesInClasspathRoot(uri, predicate, className -> true));
    }

    /**
     * Method to scan the Java class path and return a list of lasses matching the package name and
     * Predicate
     *
     * @param packageName packageName
     * @param predicate predicate
     * @return a List of Classes
     */
    public static List<Class<?>> findClasses(String packageName, Predicate<Class<?>> predicate) {
        ArgumentSupport.notNullOrEmpty(packageName, "packageName is null", "packageName is empty");
        ArgumentSupport.notNull(predicate, "predicate is null");

        return new ArrayList<>(
                ReflectionSupport.findAllClassesInPackage(
                        packageName.trim(), predicate, className -> true));
    }

    /**
     * Method to scan the Java class path and return a list of URLs matching the name
     *
     * @param regex regex
     * @return a List of URLs
     * @throws IOException IOException
     */
    public static List<URL> findResources(String regex) throws IOException {
        ArgumentSupport.notNullOrEmpty(regex, "regex is null", "regex is empty");

        Pattern pattern = Pattern.compile(regex.trim());
        List<URL> resourceUrls = new ArrayList<>();

        for (URI uri : getClasspathURIs()) {
            Path path = Paths.get(uri);
            if (Files.isDirectory(path)) {
                scanDirectory(pattern, path, resourceUrls);
            } else if (path.toString().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
                scanJarFile(pattern, path, resourceUrls);
            }
        }

        return resourceUrls;
    }

    /**
     * Method to scan a directory
     *
     * @param pattern pattern
     * @param directoryPath directoryPath
     * @param resourceUrls resourceUrls
     * @throws IOException IOException
     */
    private static void scanDirectory(Pattern pattern, Path directoryPath, List<URL> resourceUrls)
            throws IOException {
        Files.walkFileTree(directoryPath, new PathSimpleFileVisitor(pattern, resourceUrls));
    }

    /**
     * Method to scan a Jar file
     *
     * @param pattern pattern
     * @param jarPath jarPath
     * @param resourceUrls resourceUrls
     * @throws IOException IOException
     */
    private static void scanJarFile(Pattern pattern, Path jarPath, List<URL> resourceUrls)
            throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile().getPath())) {
            Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
            while (jarEntryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = jarEntryEnumeration.nextElement();
                if (!jarEntry.isDirectory() && pattern.matcher(jarEntry.getName()).matches()) {
                    resourceUrls.add(new URL("jar:" + jarPath.toUri() + "!/" + jarEntry.getName()));
                }
            }
        }
    }

    /** Class to implement PathSimpleFileVisitor */
    private static class PathSimpleFileVisitor extends SimpleFileVisitor<Path> {

        private final Pattern pattern;
        private final List<URL> foundUrls;

        /**
         * Constructor
         *
         * @param pattern pattern
         * @param foundUrls foundUrls
         */
        public PathSimpleFileVisitor(Pattern pattern, List<URL> foundUrls) {
            this.pattern = pattern;
            this.foundUrls = foundUrls;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
                throws IOException {
            if (basicFileAttributes.isRegularFile()
                    && pattern.matcher(path.getFileName().toString()).matches()) {
                foundUrls.add(path.toUri().toURL());
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException ioException) {
            return FileVisitResult.CONTINUE;
        }
    }
}
