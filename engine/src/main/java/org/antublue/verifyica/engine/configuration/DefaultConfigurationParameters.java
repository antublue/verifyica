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

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.antublue.verifyica.api.Configuration;
import org.antublue.verifyica.engine.support.ArgumentSupport;
import org.junit.platform.engine.ConfigurationParameters;

/** Class to implement DefaultConfigurationParameters */
@SuppressWarnings("deprecation")
public class DefaultConfigurationParameters implements ConfigurationParameters {

    private final Configuration configuration;

    /**
     * Constructor
     *
     * @param configuration configuration
     */
    public DefaultConfigurationParameters(Configuration configuration) {
        ArgumentSupport.notNull(configuration, "configuration is null");

        this.configuration = configuration;
    }

    @Override
    public Optional<String> get(String key) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        return configuration.getOptional(key.trim());
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");

        return Optional.ofNullable(configuration.get(key.trim())).map("true"::equals);
    }

    @Override
    public <T> Optional<T> get(String key, Function<String, T> transformer) {
        ArgumentSupport.notNullOrEmpty(key, "key is null", "key is empty");
        ArgumentSupport.notNull(transformer, "transformer is null");

        String value = configuration.get(key.trim());
        return value != null ? Optional.ofNullable(transformer.apply(value)) : Optional.empty();
    }

    @Override
    public int size() {
        return configuration.size();
    }

    @Override
    public Set<String> keySet() {
        return configuration.keySet();
    }
}
