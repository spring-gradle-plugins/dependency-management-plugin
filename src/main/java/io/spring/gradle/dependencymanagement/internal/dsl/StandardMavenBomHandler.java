/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.HashMap;
import java.util.Map;

import io.spring.gradle.dependencymanagement.dsl.MavenBomHandler;
import io.spring.gradle.dependencymanagement.internal.properties.MapPropertySource;
import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;

/**
 * Standard implementation of {@link MavenBomHandler}.
 *
 * @author Andy Wilkinson
 */
class StandardMavenBomHandler implements MavenBomHandler {

    private final Map<String, String> bomProperties = new HashMap<String, String>();

    @Override
    public void bomProperty(String name, String value) {
            this.bomProperties.put(name, value);
        }

    @Override
    public void bomProperties(Map<String, String> properties) {
        putAll(properties, this.bomProperties);
    }

    private void putAll(Map<? extends CharSequence, ? extends CharSequence> source, Map<String, String> target) {
        for (Map.Entry<? extends CharSequence, ? extends CharSequence> entry: source.entrySet()) {
            target.put(entry.getKey().toString(), entry.getValue().toString());
        }
    }

    PropertySource getBomProperties() {
        return new MapPropertySource(this.bomProperties);
    }

}
