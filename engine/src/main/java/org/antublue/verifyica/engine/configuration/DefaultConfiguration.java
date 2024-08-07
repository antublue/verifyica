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

package org.antublue.verifyica.engine.configuration;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import org.antublue.verifyica.api.Configuration;
import org.antublue.verifyica.engine.exception.EngineConfigurationException;
import org.antublue.verifyica.engine.support.ArgumentSupport;

/** Class to implement DefaultConfiguration */
public class DefaultConfiguration implements Configuration {

    private static final String VERIFYICA_CONFIGURATION_TRACE = "VERIFYICA_CONFIGURATION_TRACE";

    private static final String VERIFYICA_PROPERTIES_FILENAME = "verifyica.properties";

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private static final boolean IS_TRACE_ENABLED =
            Constants.TRUE.equals(System.getenv().get(VERIFYICA_CONFIGURATION_TRACE));

    private final Map<String, String> map;
    private final ReadWriteLock readWriteLock;

    /** Constructor */
    private DefaultConfiguration() {
        map = new TreeMap<>();
        readWriteLock = new ReentrantReadWriteLock(true);

        loadConfiguration();
    }

    /**
     * Constructor
     *
     * @param map map
     */
    private DefaultConfiguration(TreeMap<String, String> map) {
        this.map = map;
        readWriteLock = new ReentrantReadWriteLock(true);
    }

    @Override
    public Optional<String> put(String key, String value) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        try {
            getReadWriteLock().writeLock().lock();
            return Optional.ofNullable(map.put(key.trim(), value));
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public String get(String key) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        try {
            getReadWriteLock().readLock().lock();
            return map.get(key.trim());
        } finally {
            getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public Optional<String> getOptional(String key) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        try {
            getReadWriteLock().readLock().lock();
            return Optional.ofNullable(map.get(key.trim()));
        } finally {
            getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public Optional<String> computeIfAbsent(String key, Function<String, String> transformer) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");
        ArgumentSupport.notNull(transformer, "transformer is null");

        try {
            getReadWriteLock().writeLock().lock();
            return Optional.ofNullable(map.computeIfAbsent(key.trim(), transformer));
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public boolean containsKey(String key) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        try {
            getReadWriteLock().readLock().lock();
            return map.containsKey(key.trim());
        } finally {
            getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public String remove(String key) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        try {
            getReadWriteLock().writeLock().lock();
            return map.remove(key.trim());
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public Optional<String> removeOptional(String key) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        try {
            getReadWriteLock().writeLock().lock();
            return Optional.ofNullable(map.remove(key.trim()));
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public int size() {
        try {
            getReadWriteLock().readLock().lock();
            return map.size();
        } finally {
            getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Configuration clear() {
        try {
            getReadWriteLock().writeLock().lock();
            map.clear();
            return this;
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public Set<String> keySet() {
        try {
            getReadWriteLock().readLock().lock();
            return new TreeSet<>(map.keySet());
        } finally {
            getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public Configuration merge(Map<String, String> map) {
        ArgumentSupport.notNull(map, "map is null");

        try {
            getReadWriteLock().writeLock().lock();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null
                        && !key.trim().isEmpty()
                        && value != null
                        && !value.trim().isEmpty()) {
                    this.map.put(key.trim(), value.trim());
                }
            }
            return this;
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public Configuration merge(Configuration configuration) {
        ArgumentSupport.notNull(configuration, "configuration is null");

        try {
            configuration.getReadWriteLock().readLock().lock();
            getReadWriteLock().writeLock().lock();
            configuration
                    .keySet()
                    .forEach(
                            key ->
                                    configuration
                                            .getOptional(key)
                                            .ifPresent(value -> map.put(key, value)));
            return this;
        } finally {
            getReadWriteLock().writeLock().unlock();
            configuration.getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public Configuration replace(Map<String, String> map) {
        ArgumentSupport.notNull(map, "map is null");

        try {
            getReadWriteLock().writeLock().lock();
            clear();
            return merge(map);
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public Configuration replace(Configuration configuration) {
        ArgumentSupport.notNull(configuration, "configuration is null");

        try {
            configuration.getReadWriteLock().readLock().lock();
            getReadWriteLock().writeLock().lock();
            clear();
            return merge(configuration);
        } finally {
            getReadWriteLock().writeLock().unlock();
            configuration.getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public Configuration duplicate() {
        try {
            getReadWriteLock().readLock().lock();
            return new DefaultConfiguration(new TreeMap<>(map));
        } finally {
            getReadWriteLock().readLock().unlock();
        }
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    @Override
    public String toString() {
        return "DefaultConfiguration{" + "map=" + map + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultConfiguration that = (DefaultConfiguration) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(map);
    }

    /**
     * Method to get a singleton instance
     *
     * @return the singleton instance
     */
    public static DefaultConfiguration getInstance() {
        return SingletonHolder.SINGLETON;
    }

    /** Class to hold the singleton instance */
    private static class SingletonHolder {

        /** The singleton instance */
        private static final DefaultConfiguration SINGLETON = new DefaultConfiguration();
    }

    /** Method to load configuration */
    private void loadConfiguration() {
        if (IS_TRACE_ENABLED) {
            trace("loadConfiguration()");
        }

        try {
            // Get the properties file from a system property
            Optional<File> optional =
                    Optional.ofNullable(System.getProperties().get(VERIFYICA_PROPERTIES_FILENAME))
                            .map(value -> new File(value.toString()).getAbsoluteFile());

            // If a system property didn't exist, scan from the
            // current directory to the root directory
            if (!optional.isPresent()) {
                optional = find(Paths.get("."), VERIFYICA_PROPERTIES_FILENAME);
            }

            if (optional.isPresent()) {
                if (IS_TRACE_ENABLED) {
                    trace("loading [" + optional.get().getAbsolutePath() + "]");
                }

                Properties properties = new Properties();

                try (Reader reader =
                        Files.newBufferedReader(optional.get().toPath(), StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }

                if (IS_TRACE_ENABLED) {
                    trace("loaded [" + optional.get().getAbsolutePath() + "]");
                }

                properties.forEach((key, value) -> map.put((String) key, (String) value));

                map.put(VERIFYICA_PROPERTIES_FILENAME, optional.get().getAbsolutePath());
            } else {
                if (IS_TRACE_ENABLED) {
                    trace("no configuration properties file found");
                }
            }
        } catch (IOException e) {
            throw new EngineConfigurationException("Exception loading configuration properties", e);
        }

        if (IS_TRACE_ENABLED) {
            trace("configuration properties ...");
            map.keySet()
                    .forEach(
                            (key) ->
                                    trace(
                                            "configuration property ["
                                                    + key
                                                    + "] = ["
                                                    + map.get(key)
                                                    + "]"));
        }
    }

    /**
     * Method to find a properties file, searching the working directory, then parent directories
     * toward the root
     *
     * @param path path
     * @param filename filename
     * @return a optional containing a File
     */
    private static Optional<File> find(Path path, String filename) {
        Path currentPath = path.toAbsolutePath().normalize();

        while (true) {
            if (IS_TRACE_ENABLED) {
                String currentPathString = currentPath.toString();
                if (!currentPathString.endsWith("/")) {
                    currentPathString += "/";
                }

                if (IS_TRACE_ENABLED) {
                    trace("searching path [" + currentPathString + "]");
                }
            }

            File file = new File(currentPath.toAbsolutePath() + File.separator + filename);
            if (file.exists() && file.isFile() && file.canRead()) {
                if (IS_TRACE_ENABLED) {
                    trace("found [" + file.getAbsoluteFile() + "]");
                }
                return Optional.of(file);
            }

            currentPath = currentPath.getParent();
            if (currentPath == null) {
                break;
            }

            currentPath = currentPath.toAbsolutePath().normalize();
        }

        return Optional.empty();
    }

    /**
     * Method to log a TRACE message
     *
     * @param message message
     */
    private static void trace(String message) {
        if (IS_TRACE_ENABLED) {
            String dateTime;

            synchronized (SIMPLE_DATE_FORMAT) {
                dateTime = SIMPLE_DATE_FORMAT.format(new Date());
            }

            System.out.println(
                    dateTime
                            + " | "
                            + Thread.currentThread().getName()
                            + " | "
                            + "TRACE"
                            + " | "
                            + DefaultConfiguration.class.getName()
                            + " | "
                            + message
                            + " ");
        }
    }
}
